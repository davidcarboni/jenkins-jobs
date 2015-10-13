package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.values.Environment;
import com.github.davidcanboni.jenkins.values.GitRepo;
import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.github.davidcanboni.jenkins.values.GitRepo.*;

/**
 * Handles jobs in the Maven Build category.
 */
public class ContainerJobs {

    public static Document forRepo(URL gitUrl) throws IOException {
        Document document = getTemplate();
        setGitUrl(gitUrl, document);
        return document;
    }

    public static Document forRepo(GitRepo gitRepo, Environment environment) throws IOException {
        Document document = getTemplate();
        setGitUrl(gitRepo.url, document);
        setBranch(environment.name(), document);
        boolean website = gitRepo == babbage || gitRepo == zebedeeReader || gitRepo == thetrain;
        boolean publishing = gitRepo == babbage || gitRepo == florence || gitRepo == GitRepo.zebedee || gitRepo == thetrain || gitRepo == brian;
        setDownstreamDeployJobs(environment, document, website, publishing);
        if (gitRepo.nodeJs) {
            addNodeBuildStep(document, gitRepo);
        }
        removeImageCommand(gitRepo, environment, document);
        tagImageCommand(gitRepo, environment, document);
        createImageCommand(gitRepo, environment, document);
        pushImageCommand(gitRepo, environment, document);
        return document;
    }

    private static Document getTemplate() throws IOException {
        return ResourceUtils.getXml(Templates.configContainer);
    }

    private static void setGitUrl(URL gitUrl, Document template) throws IOException {
        Xml.setTextValue(template, "//hudson.plugins.git.UserRemoteConfig/url", gitUrl.toString());
    }

    private static void setBranch(String branch, Document template) throws IOException {
        Xml.setTextValue(template, "//hudson.plugins.git.BranchSpec/name", "*/" + branch);
    }

    private static void setDownstreamDeployJobs(Environment environment, Document template, boolean website, boolean publishing) throws IOException {
        List<String> jobNames = new ArrayList<>();
        if (website)
            jobNames.add(DeployJobs.jobNameWebsite(environment));
        if (publishing)
            jobNames.add(DeployJobs.jobNamePublishing(environment));
        String childProjects = StringUtils.join(jobNames, ", ");
        Xml.setTextValue(template, "//publishers/hudson.tasks.BuildTrigger/childProjects", childProjects);
    }

    private static void removeImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registryRepo;
        String image = gitRepo.toString();
        String tag = environment.name();
        String imageTag = registry + "/" + image + ":" + tag + "_previous";
        Xml.setTextValues(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.RemoveImageCommand']/imageName", imageTag);
    }

    private static void tagImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registryRepo;
        String image = gitRepo.toString();
        String tag = environment.name() + "_previous";
        String imageName = registry + "/" + image;
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.TagImageCommand']/image", imageName + ":" + environment.name());
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.TagImageCommand']/repository", imageName);
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.TagImageCommand']/tag", tag);
    }

    private static void createImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registryRepo;
        String image = gitRepo.toString();
        String tag = environment.name();
        String imageTag = registry + "/" + image + ":" + tag;
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.CreateImageCommand']/imageTag", imageTag);
        if (gitRepo.submodule) {
            // Specially for Zebedee-Reader because it's a submodule:
            Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.CreateImageCommand']/dockerFolder", "$WORKSPACE/" + gitRepo);
        }
    }

    private static void pushImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registryRepo;
        String image = registry + "/" + gitRepo.toString();
        String tag = environment.name();
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.PushImageCommand']/image", image);
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.PushImageCommand']/tag", tag);
    }

    private static void addNodeBuildStep(Document template, GitRepo gitRepo) {

        Node builders = Xml.getNode(template, "/project/builders");

        // Generate the additional nodes
        Node task = template.createElement("hudson.tasks.Shell");
        Node command = template.createElement("command");
        Node text;
        if (gitRepo == florence)
            text = template.createTextNode("npm install --prefix ./src/main/web/florence  --unsafe-perm");
        else
            text = template.createTextNode("npm install --prefix ./src/main/web  --unsafe-perm");

        // Append nodes
        builders.insertBefore(task, builders.getFirstChild());
        task.appendChild(command);
        command.appendChild(text);
        builders.normalize();
    }


    public static void create(GitRepo gitRepo, Environment environment) throws IOException, URISyntaxException {

        try (Http http = new Http()) {

            http.addHeader("Content-Type", "application/xml");
            String jobName = jobName(gitRepo, environment);
            Document config = forRepo(gitRepo, environment);

            if (!Jobs.exists(jobName)) {

                System.out.println("Creating Maven job " + jobName);
                create(jobName, config, http);

            } else {

                System.out.println("Updating Maven job " + jobName);
                Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
                update(jobName, config, http, endpoint);

            }

        }
    }

    public static String jobName(GitRepo gitRepo, Environment environment) {
        return WordUtils.capitalize(gitRepo.toString()) + " container (" + environment.name() + ")";
    }

    private static void create(String jobName, Document config, Http http) throws IOException {


        // Post the config XML to create the job
        Endpoint endpoint = Jobs.createItem.setParameter("name", jobName);
        Response<String> response = http.post(endpoint, config, String.class);
        if (response.statusLine.getStatusCode() != 200) {
            System.out.println(response.body);
            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + response.statusLine.getReasonPhrase());
        }
    }

    private static void update(String jobName, Document config, Http http, Endpoint endpoint) throws IOException {

        // Post the config XML to update the job
        Response<String> response = http.post(endpoint, config, String.class);
        if (response.statusLine.getStatusCode() != 200) {
            System.out.println(response.body);
            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + response.statusLine.getReasonPhrase());
        }
    }


    /**
     * TODO: registry credentials
     *
     * @param args
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void main(String[] args) throws IOException, URISyntaxException {

        // Loop through the matrix of combinations and set up the jobs:
        for (Environment environment : Environment.values()) {
            for (GitRepo gitRepo : GitRepo.values()) {
                create(gitRepo, environment);
            }
        }
    }
}
