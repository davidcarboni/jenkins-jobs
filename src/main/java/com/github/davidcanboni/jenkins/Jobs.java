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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by david on 08/07/2015.
 */
public class Jobs {

    static Host jenkins = new Host(Environments.jenkins());
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

    static Document buildJob(String name, String githubUrl, String branch) throws IOException {
        Document result = null;


        if (StringUtils.isNotBlank(name)) {
            try (Http http = new Http()) {

                String jobName = "Build " + name + " (" + branch + ")";

                Document config;
                if (!exists(jobName)) {

                    System.out.println("Creating job " + jobName + " for " + name + "/" + branch + " at " + githubUrl);
                    config = MavenJobs.forRepo(githubUrl, branch);

                } else {

                    System.out.println("Updating job " + jobName + " for " + name + "/" + branch + " at " + githubUrl);
                    Endpoint configXml = new Endpoint(jenkins, "/job/" + jobName + "/config.xml");
                    Response<Path> xml = http.get(configXml);
                    if (xml.statusLine.getStatusCode() != 200)
                        throw new RuntimeException("Error reading configuration for job " + jobName + ": " + xml.statusLine.getReasonPhrase());
                    config = Xml.fromFile(xml.body);

                }

                // Configure the job
                MavenJobs.forRepo(githubUrl, branch);
                System.out.println("Configured job:");
                System.out.println(Xml.toString(config));

                // Post the config XML to update the job
                http.addHeader("Content-Type", "application/xml");
                System.out.println(Xml.toString(config));
                Endpoint endpoint = createItem.setParameter("name", jobName);
                Response<String> create = http.post(endpoint, config, String.class);
                //Path temp = Xml.toFile(config);
                //Response<String> create = http.post(createItem.setParameter("name", jobName), temp, String.class);
                if (create.statusLine.getStatusCode() != 200) {
                    System.out.println(create.body);
                    throw new RuntimeException("Error setting configuration for job " + jobName + ": " + create.statusLine.getReasonPhrase());
                }

            }
        }

        return result;
    }

    static Document getConfig(String jobName) throws IOException {
        Document result = null;


        if (StringUtils.isNotBlank(jobName)) {
            try (Http http = new Http()) {

                Document config;
                if (!exists(jobName)) {

                    System.out.println("Job " + jobName + " not found?");

                } else {

                    System.out.println("Reading job config for " + jobName);
                    Endpoint configXml = new Endpoint(jenkins, "/job/" + jobName + "/config.xml");
                    Response<Path> xml = http.get(configXml);
                    if (xml.statusLine.getStatusCode() != 200)
                        throw new RuntimeException("Error reading configuration for job " + jobName + ": " + xml.statusLine.getReasonPhrase());
                    config = Xml.fromFile(xml.body);

                }

            }
        }

        return result;
    }


    public static void main(String[] args) throws IOException, URISyntaxException {

        List<Item> jobs = listJobs();

        for (Item job : jobs) {
            Document config = getConfig(job.name);
            Path path = Paths.get("src/main/resources/jobs/" + sanitise(job.name));
            Path temp = Xml.toFile(config);
            if (Files.exists(path)) Files.delete(path);
            Files.move(temp, path);
        }

//        getConfig("Monitor Beta");
//        System.exit(0);
//
//        String[] branches = new String[]{"develop", "staging", "live"};
//        Map<String, String> projects = new HashMap<>();
//        projects.put("Babbage", "https://github.com/ONSdigital/babbage.git");
//        projects.put("Florence", "https://github.com/ONSdigital/florence.git");
//        projects.put("Zebedee", "https://github.com/Carboni/zebedee.git");
//        projects.put("Brian", "https://github.com/thomasridd/project-brian.git");
//
//        // Loop through the matrix of combinations and set up the jobs:
//        for (Map.Entry<String, String> entry : projects.entrySet()) {
//            for (String branch : branches) {
//                String name = entry.getKey();
//                String githubUrl = entry.getValue();
//                buildJob(name, githubUrl, branch);
//            }
//        }
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
