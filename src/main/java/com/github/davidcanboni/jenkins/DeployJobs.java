package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.values.Environment;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Handles jobs in the Docker Library Images category.
 */
public class DeployJobs {

    static String xpathCommand = "//builders/hudson.tasks.Shell/command";
    static String xpathTag = "//builders/org.jenkinsci.plugins.dockerbuildstep.DockerBuilder/dockerCmd/tag";

//    public static Document setCommand(URL url, Environment environment) throws IOException, URISyntaxException {
//        Document document = ResourceUtils.getXml(Templates.configDeploy);
//        String command = "ssh ubuntu@"+url.getHost()+" ./deploy.sh "+environment.name();
//        Xml.setTextValue(document, xpathCommand, command);
//        return document;
//    }
//
//    private  static void setUpstreamContainerBulids(Environment environment, Document template, GitRepo... gitRepos) {
//        Xml.setTextValue(template, "//jenkins.triggers.ReverseBuildTrigger/upstreamProjects",  MavenJobs.jobName(gitRepo, environment));
//    }
//
//
//    public static void create(String jobName, String image, String tag) throws IOException, URISyntaxException {
//
//        if (StringUtils.isNotBlank(jobName)) {
//            try (Http http = new Http()) {
//
//                http.addHeader("Content-Type", "application/xml");
//                Document config = setCommand(Environment., tag);
//
//                if (!Jobs.exists(jobName)) {
//
//                    System.out.println("Creating " + jobName + " - " + image + tagString);
//
//                    // Set the URL and create:
//                    create(jobName, config, http);
//
//                } else {
//
//                    System.out.println("Updating job to pull library image " + name + " - " + image + tagString);
//                    Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
//                    Response<Path> xml = http.get(endpoint);
//                    if (xml.statusLine.getStatusCode() != 200)
//                        throw new RuntimeException("Error reading configuration for job " + jobName + ": " + xml.statusLine.getReasonPhrase());
//                    Document config = Xml.fromFile(xml.body);
//
//                    // Set the URL and update:
//                    setImage(image, tag, config);
//                    update(jobName, config, http, endpoint);
//
//                }
//
//            }
//        }
//    }
//
//    private static void create(String jobName, Document config, Http http) throws IOException {
//
//
//        // Post the config XML to create the job
//        Endpoint endpoint = Jobs.createItem.setParameter("name", jobName);
//        Response<String> create = http.post(endpoint, config, String.class);
//        if (create.statusLine.getStatusCode() != 200) {
//            System.out.println(create.body);
//            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + create.statusLine.getReasonPhrase());
//        }
//    }
//
//    private static void update(String jobName, Document config, Http http, Endpoint endpoint) throws IOException {
//
//        // Post the config XML to update the job
//        Response<String> create = http.post(endpoint, config, String.class);
//        if (create.statusLine.getStatusCode() != 200) {
//            System.out.println(create.body);
//            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + create.statusLine.getReasonPhrase());
//        }
//    }


    public static String jobNameWebsite(Environment environment) {
        return "Deploy website (" + environment.name() + ")";
    }

    public static String jobNamePublishing(Environment environment) {
        return "Deploy publishing (" + environment.name() + ")";
    }


    /**
     * @param args
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void main(String[] args) throws IOException, URISyntaxException {
    }
}
