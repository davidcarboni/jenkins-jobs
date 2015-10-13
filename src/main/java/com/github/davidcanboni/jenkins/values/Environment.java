package com.github.davidcanboni.jenkins.values;

import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by david on 15/07/2015.
 */
public enum Environment {

    develop("http", "develop.lifeinteal.com", true, "http", "publishing.develop.lifeinteal.com", true, new String[]{"ubuntu@carb.onl"}, new String[]{"ubuntu@carboni.uk"}),
    staging("http", "staging.lifeinteal.com", true, "http", "publishing.staging.lifeinteal.com", true, new String[]{"davidcarboni@10.13.26.2", "davidcarboni@10.13.26.3", "ubuntu@carb.onl"}, new String[]{"davidcarboni@10.13.26.50", "ubuntu@carboni.uk"}),
    live("http", "lifeinteal.com", false, "http", "publishing.lifeinteal.com", true, new String[]{"davidcarboni@10.13.26.2", "davidcarboni@10.13.26.3", "ubuntu@carb.onl"}, new String[]{"davidcarboni@10.13.26.50", "ubuntu@carboni.uk"}),
    sandpit("http", "sandpit.lifeinteal.com", true, "http", "publishing.sandpit.lifeinteal.com", true, new String[]{"ubuntu@carb.onl"}, new String[]{"ubuntu@carboni.uk"});
    public static String registryRepo = "onsdigital";

    // Cross-environment URLs:
    private static URL registry;
    private static URL jenkins;
    private static URL nexus;

    // Environment-specific URLs:
    private URL website;
    private URL publishing;

    // Credentials:
    private static String username = System.getProperty("username", "dougal");
    private static String password = System.getProperty("password", "ermintrude");

    // Cross-environment values:
    private static String publishingPath = "/florence/index.html";
    private static String registryProtocol = "https";
    private static String jenkinsProtocol = "http";
    private static String nexusProtocol = "http";
    private static String registryDomain = "hub.docker.com";
    private static String jenkinsDomain = "build.lifeinteal.com";
    private static String nexusDomain = "nexus.lifeinteal.com";
    private static boolean registryRequiresCredentials = false;
    private static boolean jenkinsRequiresCredentials = true;
    private static boolean nexusRequiresCredentials = false;

    // Environment-dependen values
    private String websiteProtocol;
    private String publishingProtocol;
    private String websiteDomain;
    private String publishingDomain;
    public String[] websiteTargets;
    public String[] publishingTargets;
    private boolean websiteRequiresCredentials;
    private boolean publishingRequiresCredentials;

    public URL website() {
        if (website == null) {
            website = toUrl(websiteProtocol, websiteDomain, websiteRequiresCredentials);
        }
        return website;
    }

    public URL publishing() {
        if (publishing == null) {
            publishing = toUrl(publishingProtocol, publishingDomain, publishingRequiresCredentials, publishingPath);
        }
        return publishing;
    }

    public static URL jenkins() {
        if (jenkins == null) {
            jenkins = toUrl(jenkinsProtocol, jenkinsDomain, jenkinsRequiresCredentials);
        }
        return jenkins;
    }

    public static URL registry() {
        if (registry == null) {
            registry = toUrl(registryProtocol, registryDomain, registryRequiresCredentials);
        }
        return registry;
    }

    public static URL nexus() {
        if (nexus == null) {
            nexus = toUrl(nexusProtocol, nexusDomain, nexusRequiresCredentials);
        }
        return nexus;
    }

    static URL toUrl(String protocol, String domain, boolean credentialsRequired, String... path) {
        try {
            URIBuilder builder = new URIBuilder();
            builder.setScheme(protocol);
            builder.setHost(domain);
            if (credentialsRequired)
                builder.setUserInfo(username, password);
            if (path.length > 0)
                builder.setPath(path[0]);
            return builder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Error creating URL");
        }
    }

    Environment(String websiteProtocol, String websiteDomain, boolean websiteRequiresCredentials, String publishingProtocol, String publishingDomain, boolean publishingRequiresCredentials, String[] websiteTargets, String[] publishingTargets) {
        this.websiteProtocol = websiteProtocol;
        this.publishingProtocol = publishingProtocol;
        this.websiteDomain = websiteDomain;
        this.publishingDomain = publishingDomain;
        this.websiteTargets = websiteTargets;
        this.publishingTargets = publishingTargets;
        this.websiteRequiresCredentials = websiteRequiresCredentials;
        this.publishingRequiresCredentials = publishingRequiresCredentials;
    }
}
