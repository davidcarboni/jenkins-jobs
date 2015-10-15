package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.json.Item;
import com.github.davidcanboni.jenkins.json.Jenkins;
import com.github.davidcanboni.jenkins.values.Environment;
import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Host;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Created by david on 08/07/2015.
 */
public class Jobs {

    static Host jenkins = new Host(Environment.jenkins());
    static Endpoint createItem = new Endpoint(jenkins, "/createItem");
    static Endpoint jobs = new Endpoint(jenkins, "/api/json");

    static boolean exists(String name) throws IOException {
        boolean result = false;

        for (Item job : listJobs()) {
            if (StringUtils.equalsIgnoreCase(job.name, name)) result = true;
        }

        return result;
    }

    static List<Item> listJobs() throws IOException {
        return jenkins().jobs;
    }

    static List<Item> listViews() throws IOException {
        return jenkins().views;
    }

    static Jenkins jenkins() throws IOException {
        try (Http http = new Http()) {
            Response<Jenkins> jenkins = http.get(jobs, Jenkins.class);
            if (jenkins.statusLine.getStatusCode() != 200) {
                if (jenkins.statusLine.getStatusCode() == HttpStatus.UNAUTHORIZED_401) {
                    System.out.println("You may need to set VM options on your run configuration: -Dusername=user -Dpassword=pass");
                }
                throw new RuntimeException(jenkins.statusLine.getReasonPhrase());
            }

            return jenkins.body;
        }
    }

    static Document getConfig(String jobName) throws IOException {
        Document result = null;


        if (StringUtils.isNotBlank(jobName)) {
            try (Http http = new Http()) {

                if (!exists(jobName)) {

                    System.out.println("Job " + jobName + " not found?");

                } else {

                    System.out.println("Reading job config for " + jobName);
                    Endpoint configXml = new Endpoint(jenkins, "/job/" + jobName + "/config.xml");
                    Response<Path> xml = http.get(configXml);
                    if (xml.statusLine.getStatusCode() != 200)
                        throw new RuntimeException("Error reading configuration for job " + jobName + ": " + xml.statusLine.getReasonPhrase());
                    result = Xml.fromFile(xml.body);

                }

            }
        }

        return result;
    }

    static void rename(String oldName, String newName) throws IOException {
        if (exists(oldName)) {
            Endpoint endpoint = new Endpoint(jenkins, "/job");
            endpoint = endpoint.addPathSegment(oldName);
            endpoint = endpoint.addPathSegment("doRename");
            endpoint = endpoint.setParameter("newName", newName);
            try (Http http = new Http()) {
                http.post(endpoint, Object.class);
            }
            if (!exists(newName))
                throw new RuntimeException("Job " + newName + " not found after rename.");
        } else {
            throw new RuntimeException("Job " + oldName + " does not exist.");
        }
        // /job/$CurrentJobName/doRename?newName=$NewJobName
    }


    public static void generateConfig(String jobName, Document config) throws IOException {

        System.out.println("Creating job configuration " + jobName);
        Path source = Xml.toFile(config);
        Path target = Jobs.toPath(jobName);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void uploadConfig(String jobName, Document config) throws IOException {

        try (Http http = new Http()) {

            http.addHeader("Content-Type", "application/xml");

            if (!Jobs.exists(jobName)) {

                System.out.println("Creating " + jobName);
                Jobs.create(jobName, config, http);

            } else {

                System.out.println("Updating  " + jobName);
                Jobs.update(jobName, config, http);

            }

        }
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

    private static void update(String jobName, Document config, Http http) throws IOException {

        // Post the config XML to update the job
        Endpoint endpoint = new Endpoint(jenkins, "/job/" + jobName + "/config.xml");
        Response<String> response = http.post(endpoint, config, String.class);
        if (response.statusLine.getStatusCode() != 200) {
            System.out.println(response.body);
            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + response.statusLine.getReasonPhrase());
        }
    }

    /**
     * <a href=
     * "http://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars/13293384#13293384"
     * >http://stackoverflow.com/questions/1155107/is-there-a-cross-platform-
     * java-method-to-remove-filename-special-chars/13293384#13293384</a>
     *
     * @param name The name to be sanitised so that it's suitable for use as a filename.
     * @return The sanitised version of the given name.
     */
    private static String sanitise(String name) {
        StringBuilder result = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c == '.' || Character.isJavaIdentifierPart(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static Path toPath(String jobName) {
        return Paths.get("src/main/resources/jobs/" + sanitise(jobName));
    }

}
