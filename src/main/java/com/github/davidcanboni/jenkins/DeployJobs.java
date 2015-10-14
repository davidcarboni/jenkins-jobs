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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles jobs in the Docker Library Images category.
 */
public class DeployJobs {

    static String xpathCommand = "//builders/hudson.tasks.Shell/command";
    static String xpathUpstreamJobs = "//triggers/jenkins.triggers.ReverseBuildTrigger/upstreamProjects";

    public static Document getTemplate() throws IOException {
        Document document = ResourceUtils.getXml(Templates.configDeploy);
        return document;
    }

    public static Document addDeploymentTargets(Document template, String[] targets, Environment environment) throws IOException {

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

    public static Document setUpstreamProjectsPublishing(Document template, Environment environment) throws IOException {
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

    public static Document setUpstreamProjectsWebsite(Document template, Environment environment) throws IOException {
        List<String> upstreamJobs = new ArrayList<>();
        upstreamJobs.add(ContainerJobs.jobName(GitRepo.babbage, environment));
        // NB this needs to be Zebedee-Reader
        upstreamJobs.add(ContainerJobs.jobName(GitRepo.zebedeeReader, environment));
        upstreamJobs.add(ContainerJobs.jobName(GitRepo.thetrain, environment));
        String list = StringUtils.join(upstreamJobs, ", ");
        Xml.setTextValue(template, xpathUpstreamJobs, list);
        return template;
    }


    public static String jobNameWebsite(Environment environment) {
        return "Deploy website (" + environment.name() + ")";
    }

    public static String jobNamePublishing(Environment environment) {
        return "Deploy publishing (" + environment.name() + ")";
    }

    public static Map<String, List<String>> jobNames() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("website", new ArrayList<String>());
        result.put("publishing", new ArrayList<String>());

        for (Environment environment : Environment.values()) {
            result.get("website").add(jobNameWebsite(environment));
            result.get("publishing").add(jobNamePublishing(environment));
        }

        return result;
    }

    static void generateConfig(Environment environment, boolean publishing) throws IOException {

        Document config = getTemplate();
        config.setXmlStandalone(true);
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

        Jobs.generateConfig(jobName, config);
    }


    /**
     * Generates the Jenkins XML configuration files for deployment jobs.
     *
     * @throws IOException
     */
    public static void generateConfig() throws IOException {
        for (Environment environment : Environment.values()) {
            generateConfig(environment, false);
            generateConfig(environment, true);
        }
    }

}
