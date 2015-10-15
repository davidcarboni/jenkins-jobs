package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.json.Item;
import com.github.davidcanboni.jenkins.xml.Xml;
import org.w3c.dom.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Updates all Jenkins jobs.
 * <p/>
 * To get this class to run, you'll need to pass the username and password as properties on the command line.
 * <p/>
 * If you're using intellij, you'll need to edit the run configuration and add something like the following to the VM options:
 * <p/>
 * -Dusername=user -Dpassword=pass
 */
public class Main {

    static String integrationTests = "Integration tests (develop)";

    static void generateConfigurations() throws InterruptedException, IOException {
        ContainerJobs.generateConfig();
        DeployJobs.generateConfig();
    }

    static void uploadConfigurations() throws InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(5);

        List<String> jobs = new ArrayList<>();
        jobs.addAll(ContainerJobs.jobNames());
        Map<String, List<String>> deployJobs = DeployJobs.jobNames();
        jobs.addAll(deployJobs.get("website"));
        jobs.addAll(deployJobs.get("publishing"));
        jobs.add(integrationTests);

        for (String job : jobs) {
            final String referenceJob = job;
            executorService.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        Document config = Xml.fromFile(Jobs.toPath(referenceJob));
                        Jobs.uploadConfig(referenceJob, config);
                    } catch (Exception e) {
                        throw new RuntimeException("Error for " + referenceJob, e);
                    }
                }
            });

        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
    }



    /**
     * Downloads the config.xml of each job on the Jenkins server.
     *
     * @throws IOException If an error occurs in downloading the job configuration.
     */
    public static void downloadConfigurations() throws IOException, InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(5);

        List<Item> jobs = Jobs.listJobs();

        for (Item job : jobs) {
            final Item referenceJob = job;
            executorService.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        Document config = Jobs.getConfig(referenceJob.name);
                        Path path = Jobs.toPath(referenceJob.name);
                        Path temp = Xml.toFile(config);
                        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Error for " + referenceJob, e);
                    }
                }
            });

        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        // Generate job configurations:
        generateConfigurations();

        // Update the jobs on Jenkins:
        uploadConfigurations();

        // Re-download the configurations to get them
        // in the exact format Jenkins updates them to:
        downloadConfigurations();
    }
}
