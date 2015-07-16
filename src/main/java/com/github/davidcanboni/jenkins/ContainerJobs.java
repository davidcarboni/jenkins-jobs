package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
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

    public static Document forRepo(URL gitUrl, Environment environment) throws IOException {
        Document document = getTemplate();
        setGitUrl(gitUrl, document);
        setBranch(environment.name(), document);
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


    public static void create(GitRepo gitRepo, Environment environment) throws IOException, URISyntaxException {

        try (Http http = new Http()) {

            http.addHeader("Content-Type", "application/xml");
            String jobName = jobName(gitRepo, environment);
            Document config = forRepo(gitRepo.url, environment);

            if (!Jobs.exists(jobName)) {

                System.out.println("Creating Maven job " + jobName);

                // Set the URL and create:
                //create(jobName, config, http);

            } else {

                System.out.println("Updating Maven job " + jobName);
                Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
                //update(jobName, config, http, endpoint);

            }

        }
    }

    public static String jobName(GitRepo gitRepo, Environment environment) {
        return WordUtils.capitalize(gitRepo.name()) + " container (" + environment.name() + ")";
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
            for (GitRepo gitRepo : GitRepo.values()) {
                create(gitRepo, environment);
            }
        }

    }
}