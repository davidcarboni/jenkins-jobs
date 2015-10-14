package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.json.Item;
import com.github.davidcanboni.jenkins.values.Environment;
import com.github.davidcanboni.jenkins.values.GitRepo;
import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.w3c.dom.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        //MonitorJobs.generateConfig();
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

    public static void main(String[] args) throws IOException, InterruptedException {

        // Configure jobs:
        generateConfigurations();
        //uploadConfigurations();

        // Re-download the configurations to get them
        // in the exact format Jenkins updates them to:
        Jobs.main(args);
    }
}
