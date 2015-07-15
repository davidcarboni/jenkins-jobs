package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles jobs in the Maven Build category.
 */
public class MavenJobs  {

    public static Document forRepo(String gitUrl) throws IOException {
        Document document = getTemplate();
        setGitUrl(gitUrl, document);
        return document;
    }

    public static Document forRepo(String gitUrl, String branch) throws IOException {
        Document document = getTemplate();
        setGitUrl(gitUrl, document);
        setBranch(branch, document);
        return document;
    }

    private static Document getTemplate() throws IOException {
        return ResourceUtils.getXml(Templates.configMaven);
    }

    private static void setGitUrl(String gitUrl, Document template) throws IOException {
        Xml.setTextValue(template, "//hudson.plugins.git.UserRemoteConfig/url", gitUrl);
    }

    private static void setBranch(String branch, Document template) throws IOException {
        Xml.setTextValue(template, "//hudson.plugins.git.BranchSpec/name", branch);
    }




    public static void create(String name, String gitUrl, String branch) throws IOException, URISyntaxException {

        if (StringUtils.isNotBlank(name)) {
            try (Http http = new Http()) {

                http.addHeader("Content-Type", "application/xml");
                String jobName = "Maven build " + name;
                Document config = forRepo(gitUrl, branch);

                if (!Jobs.exists(jobName)) {

                    System.out.println("Creating Maven job " + jobName);

                    // Set the URL and create:
                    create(jobName, config, http);

                } else {

                    System.out.println("Updating Maven job " + jobName);
                    Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
//                    Response<Path> xml = http.get(endpoint);
//                    if (xml.statusLine.getStatusCode() != 200)
//                        throw new RuntimeException("Error reading configuration for job " + jobName + ": " + xml.statusLine.getReasonPhrase());
//                    Document config = Xml.fromFile(xml.body);
//
//                    // Set the URL and update:
//                    setImage(image, tag, config);
                    update(jobName, config, http, endpoint);

                }

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


    public static void main(String[] args) throws IOException, URISyntaxException {
        Map<String, String> images = new HashMap<>();
        Map<String, String> tags = new HashMap<>();

        images.put("Nginx", "nginx");
        images.put("Jenkins", "jenkins");
        images.put("Nexus", "nexus");
        tags.put("Nexus", "oss");
        images.put("Registry", "registry");

        for (Map.Entry<String, String> entry : images.entrySet()) {
            String name = entry.getKey();
            String image = entry.getValue();
            String tag = tags.get(name);
            create(name, image, tag);
        }
    }
}
