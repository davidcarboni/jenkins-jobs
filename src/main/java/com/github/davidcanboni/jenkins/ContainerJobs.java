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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

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
        setUpstreamMavenBulid(gitRepo, environment, document);
        setDownstreamDeployJobs(environment, document);
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

    private static void setUpstreamMavenBulid(GitRepo gitRepo, Environment environment, Document template) {
        Xml.setTextValue(template, "//jenkins.triggers.ReverseBuildTrigger/upstreamProjects", MavenJobs.jobName(gitRepo, environment));
    }

    private static void setDownstreamDeployJobs(Environment environment, Document template) throws IOException {
        String[] jobNames = new String[]{DeployJobs.jobNameWebsite(environment), DeployJobs.jobNamePublishing(environment)};
        String childProjects = StringUtils.join(jobNames, ", ");
        Xml.setTextValue(template, "//publishers/hudson.tasks.BuildTrigger/childProjects", childProjects);
    }
    //Deploy publishing (develop), Deploy website (develop)

    private static void removeImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registry().getHost();
        String image = gitRepo.name();
        String tag = environment.name();
        String imageTag = registry + "/" + image + ":" + tag + "_previous";
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.RemoveImageCommand']/imageName", imageTag);
    }

    private static void tagImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registry().getHost();
        String image = gitRepo.name();
        String tag = environment.name() + "_previous";
        String imageName = registry + "/" + image;
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.TagImageCommand']/image", imageName);
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.TagImageCommand']/repository", imageName);
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.TagImageCommand']/tag", tag);
    }

    private static void createImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registry().getHost();
        String image = gitRepo.name();
        String tag = environment.name();
        String imageTag = registry + "/" + image + ":" + tag;
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.CreateImageCommand']/imageTag", imageTag);
    }

    private static void pushImageCommand(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        String registry = Environment.registry().getHost();
        String image = registry + "/" + gitRepo.name();
        String tag = environment.name();
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.PushImageCommand']/image", image);
        Xml.setTextValue(template, "//dockerCmd[@class='org.jenkinsci.plugins.dockerbuildstep.cmd.PushImageCommand']/tag", tag);
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
        return WordUtils.capitalize(gitRepo.name()) + " container (" + environment.name() + ")";
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


    public static void main(String[] args) throws IOException, URISyntaxException {

        // Loop through the matrix of combinations and set up the jobs:
        for (Environment environment : Environment.values()) {
            for (GitRepo gitRepo : GitRepo.values()) {
                create(gitRepo, environment);
            }
        }

    }
}
