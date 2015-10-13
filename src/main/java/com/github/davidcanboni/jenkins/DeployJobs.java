package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.values.Environment;
import com.github.davidcanboni.jenkins.values.GitRepo;
import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles jobs in the Docker Library Images category.
 */
public class DeployJobs {

    static String xpathCommand = "//builders/hudson.tasks.Shell/command";
    static String xpathUpstreamJobs = "//triggers/jenkins.triggers.ReverseBuildTrigger/upstreamProjects";

    //static String[] websiteTargets = new String[]{"davidcarboni@10.13.26.2", "davidcarboni@10.13.26.3", "ubuntu@carb.onl"};
    //static String[] publishingTargets = new String[]{"davidcarboni@10.13.26.50", "ubuntu@carboni.uk"};

    public static Document getTemplate() throws IOException, URISyntaxException {
        Document template = ResourceUtils.getXml(Templates.configDeploy);
        return template;
    }

    public static Document addDeploymentTargets(Document template, String[] targets, Environment environment) throws IOException, URISyntaxException {

        for (String target : targets) {
            addDeploymentTarget(template, target, environment);
        }
        return template;
    }

    private static void addDeploymentTarget(Document template, String target, Environment environment) {

        Node builders = Xml.getNode(template, "/project/builders");

        // Generate the additional nodes
        Node deployment = template.createElement("hudson.tasks.Shell");
        Node command = template.createElement("command");
        Node text = template.createTextNode("ssh " + target + " ./deploy.sh " + environment.name());

        // Append nodes
        builders.appendChild(deployment);
        deployment.appendChild(command);
        command.appendChild(text);
        builders.normalize();
    }

    public static Document setUpstreamProjectsPublishing(Document template, Environment environment) throws IOException, URISyntaxException {
        List<String> upstreamJobs = new ArrayList<>();
        // Trigger a deploy for a change in any component:
        for (GitRepo gitRepo : GitRepo.values()) {
            if (gitRepo != GitRepo.zebedeeReader)
                upstreamJobs.add(ContainerJobs.jobName(gitRepo, environment));
        }
        String list = StringUtils.join(upstreamJobs, ", ");
        Xml.setTextValue(template, xpathUpstreamJobs, list);
        return template;
    }

    public static Document setUpstreamProjectsWebsite(Document template, Environment environment) throws IOException, URISyntaxException {
        List<String> upstreamJobs = new ArrayList<>();
        upstreamJobs.add(ContainerJobs.jobName(GitRepo.babbage, environment));
        // NB this needs to be Zebedee-Reader
        upstreamJobs.add(ContainerJobs.jobName(GitRepo.zebedeeReader, environment));
        upstreamJobs.add(ContainerJobs.jobName(GitRepo.thetrain, environment));
        String list = StringUtils.join(upstreamJobs, ", ");
        Xml.setTextValue(template, xpathUpstreamJobs, list);
        return template;
    }

    public static void create(Environment environment) throws IOException, URISyntaxException {

        create(environment, false);
        create(environment, true);
    }

    public static void create(Environment environment, boolean publishing) throws IOException, URISyntaxException {

        try (Http http = new Http()) {

            http.addHeader("Content-Type", "application/xml");
            Document config = getTemplate();
            String jobName;
            String[] target;
            if (publishing) {
                setUpstreamProjectsPublishing(config, environment);
                jobName = jobNamePublishing(environment);
                target = environment.publishingTargets;
            } else {
                setUpstreamProjectsWebsite(config, environment);
                jobName = jobNameWebsite(environment);
                target = environment.websiteTargets;
            }
            addDeploymentTargets(config, target, environment);

            if (!Jobs.exists(jobName)) {

                System.out.println("Creating " + jobName);
                create(jobName, config, http);

            } else {

                System.out.println("Updating  " + jobName);
                Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
                update(jobName, config, http, endpoint);

            }

        }
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
        for (Environment environment : Environment.values()) {
            create(environment);
        }
    }

}
