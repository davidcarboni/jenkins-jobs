package com.github.davidcanboni.jenkins;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Updates all Jenkins jobs.
 *
 * To get this class to run, you'll need to pass the username and password as properties on the command line.
 *
 * If you're using intellij, you'll need to edit the run configuration and add something like the following to the VM options:
 *
 * -Dusername=user -Dpassword=pass
 */
public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        ContainerJobs.main(args);
        DeployJobs.main(args);
        ContainerNodeJobs.main(args);
        MonitorJobs.main(args);
        Jobs.main(args);
    }
}
