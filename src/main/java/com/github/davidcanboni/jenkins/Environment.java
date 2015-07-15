package com.github.davidcanboni.jenkins;

import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by david on 15/07/2015.
 */
public enum Environment {

    develop("http", "develop.carb.onl", true, "http", "develop.carboni.uk", true),
    staging("http", "develop.carb.onl", true, "http", "develop.carboni.uk", true),
    live("http", "beta.ons.gov.uk", false, "http", "publishing.beta.ons.gov.uk", true);

    // Cross-environment URLs:
    private static URL registry;
    private static URL jenkins;
    private static URL nexus;

    // Environment-specific URLs:
    private URL website;
    private URL publishing;

    // Credentials:
    private static String username = System.getProperty("username", "bob");
    private static String password = System.getProperty("password", "tree");

    // Cross-environment values:
    private static String publishingPath = "/florence/index.html";
    private static String registryProtocol = "https";
    private static String jenkinsProtocol = "http";
    private static String nexusProtocol = "http";
    private static String registryDomain = "carboni.io";
    private static String jenkinsDomain = "jenkins.carboni.io";
    private static String nexusDomain = "nexus.carboni.io";
    private static boolean registryRequiresCredentials = true;
    private static boolean jenkinsRequiresCredentials = true;
    private static boolean nexusRequiresCredentials = false;

    // Environment-dependen values
    private String websiteProtocol;
    private String publishingProtocol;
    private String websiteDomain;
    private String publishingDomain;
    private boolean websiteRequiresCredentials;
    private boolean publishingRequiresCredentials;

     URL website() {
        if (website == null) {
            website = toUrl(websiteProtocol, websiteDomain, websiteRequiresCredentials);
        }
        return website;
    }

     URL publishing() {
        if (publishing == null) {
            publishing = toUrl(publishingProtocol, publishingDomain, publishingRequiresCredentials, publishingPath);
        }
        return publishing;
    }

    static URL jenkins() {
        if (jenkins == null) {
            jenkins = toUrl(jenkinsProtocol, jenkinsDomain, jenkinsRequiresCredentials);
        }
        return jenkins;
    }

    static URL registry() {
        if (registry == null) {
            registry = toUrl(registryProtocol, registryDomain, registryRequiresCredentials);
        }
        return registry;
    }

    static URL nexus() {
        if (nexus == null) {
            nexus = toUrl(nexusProtocol, nexusDomain, nexusRequiresCredentials);
        }
        return nexus;
    }

    private static URL toUrl(String protocol, String domain, boolean credentialsRequired, String... path) {
        try {
            URIBuilder builder = new URIBuilder();
            builder.setScheme(protocol);
            builder.setHost(domain);
            if (credentialsRequired)
                builder.setUserInfo(username, password);
            if (path.length>0)
                builder.setPath(path[0]);
            return builder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Error creating URL");
        }
    }

    Environment(String websiteProtocol, String websiteDomain, boolean websiteRequiresCredentials, String publishingProtocol, String publishingDomain, boolean publishingRequiresCredentials) {
        this.websiteProtocol = websiteProtocol;
        this.publishingProtocol = publishingProtocol;
        this.websiteDomain = websiteDomain;
        this.publishingDomain = publishingDomain;
        this.websiteRequiresCredentials = websiteRequiresCredentials;
        this.publishingRequiresCredentials = publishingRequiresCredentials;
    }
}
