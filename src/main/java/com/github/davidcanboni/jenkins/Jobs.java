package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.json.Item;
import com.github.davidcanboni.jenkins.json.Jenkins;
import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Host;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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


    /**
     * Downloads the config.xml of each job on the Jenkins server.
     * @param args Not used.
     * @throws IOException If an error occurs in downloading the job configuration.
     */
    public static void main(String[] args) throws IOException {

        List<Item> jobs = listJobs();

        for (Item job : jobs) {
            Document config = getConfig(job.name);
            Path path = Paths.get("src/main/resources/jobs/" + sanitise(job.name));
            Path temp = Xml.toFile(config);
            if (Files.exists(path)) Files.delete(path);
            Files.move(temp, path);
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
}
