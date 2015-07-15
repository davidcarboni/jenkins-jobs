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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles jobs in the Docker Library Images category.
 */
public class LibraryJobs  {

    static String xpathImage = "//builders/org.jenkinsci.plugins.dockerbuildstep.DockerBuilder/dockerCmd/fromImage";
    static String xpathTag = "//builders/org.jenkinsci.plugins.dockerbuildstep.DockerBuilder/dockerCmd/tag";

    public static Document setImage(String image, String tag) throws IOException, URISyntaxException {
        Document document = ResourceUtils.getXml(Templates.configLibrary);
        Xml.setTextValue(document, xpathImage, image);
        return document;
    }

    public static void setImage(String image, String tag, Document document) throws IOException, URISyntaxException {
        Xml.setTextValue(document, xpathImage, image);
        if (StringUtils.isNotBlank(tag))
            Xml.setTextValue(document, xpathTag, tag);
    }


    public static void create(String name, String image, String tag) throws IOException, URISyntaxException {

        if (StringUtils.isNotBlank(name)) {
            try (Http http = new Http()) {

                http.addHeader("Content-Type", "application/xml");

                String jobName = "Docker library image " + name;
                String tagString = StringUtils.isNotBlank(tag)?":"+tag:"";

                if (!Jobs.exists(jobName)) {

                    System.out.println("Creating job to pull library image " + name + " - " + image + tagString);

                    // Set the URL and create:
                    Document config = setImage(image, tag);
                    create(jobName, config, http);

                } else {

                    System.out.println("Updating job to pull library image " + name + " - " + image + tagString);
                    Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
                    Response<Path> xml = http.get(endpoint);
                    if (xml.statusLine.getStatusCode() != 200)
                        throw new RuntimeException("Error reading configuration for job " + jobName + ": " + xml.statusLine.getReasonPhrase());
                    Document config = Xml.fromFile(xml.body);

                    // Set the URL and update:
                    setImage(image, tag, config);
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
        images.put("Nexus", "sonatype/nexus");
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
