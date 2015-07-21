package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.values.Environment;
import com.github.davidcanboni.jenkins.values.GitRepo;
import com.github.davidcanboni.jenkins.values.JobCategory;
import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.apache.commons.lang3.text.WordUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Handles jobs in the Maven Build category.
 */
public class MavenNodeJobs {

    public static Document forRepo(GitRepo gitRepo, Environment environment) throws IOException {
        Document document = getTemplate();
        setGitUrl(gitRepo.url, document);
        setBranch(environment.name(), document);
        setContainerJob(gitRepo, environment, document);
        addNodeBuildStep(document);
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

    private static void setContainerJob(GitRepo gitRepo, Environment environment, Document template) throws IOException {
        Xml.setTextValue(template, "//publishers/hudson.tasks.BuildTrigger/childProjects", ContainerJobs.jobName(gitRepo, environment));
    }

    private static void addNodeBuildStep(Document template) {

        Node builders = Xml.getNode(template, "/project/builders");

        // Generate the additional nodes
        Node task = template.createElement("hudson.tasks.Shell");
        Node command = template.createElement("command");
        Node text = template.createTextNode("npm install --prefix ./src/main/web/florence  --unsafe-perm");

        // Append nodes
        builders.insertBefore(task, builders.getFirstChild());
        task.appendChild(command);
        command.appendChild(text);
        builders.normalize();;
    }


    public static void create(GitRepo gitRepo, Environment environment) throws IOException, URISyntaxException {

        try (Http http = new Http()) {

            http.addHeader("Content-Type", "application/xml");
            String jobName = jobName(gitRepo, environment);
            Document config = forRepo(gitRepo, environment);

            if (!Jobs.exists(jobName)) {

                System.out.println("Creating Maven job " + jobName);

                // Set the URL and create:
                //create(jobName, config, http);

            } else {

                System.out.println("Updating Maven job " + jobName);
                Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
                //update(jobName, config, http, endpoint);

            }
            System.out.println(Xml.toString(config));

        }
    }

    public static String jobName(GitRepo gitRepo, Environment environment) {
        return JobCategory.Maven + " " + WordUtils.capitalize(environment.name()) + " " + WordUtils.capitalize(gitRepo.name());
    }

    private static void create(String jobName, Document config, Http http) throws IOException {


        // Post the config XML to create the job
        Endpoint endpoint = Jobs.createItem.setParameter("name", jobName);
        Response<String> create = http.post(endpoint, config, String.class);
        if (create.statusLine.getStatusCode() != 200) {
            System.out.println(create.body);
            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + create.statusLine.getReasonPhrase());
        }
    }

    private static void update(String jobName, Document config, Http http, Endpoint endpoint) throws IOException {

        // Post the config XML to update the job
        Response<String> create = http.post(endpoint, config, String.class);
        if (create.statusLine.getStatusCode() != 200) {
            System.out.println(create.body);
            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + create.statusLine.getReasonPhrase());
        }
    }


    public static void main(String[] args) throws IOException, URISyntaxException {

        // Loop through the matrix of combinations and set up the jobs:
        for (Environment environment : Environment.values()) {
            create(GitRepo.babbage, environment);
            create(GitRepo.florence, environment);
        }

    }
}