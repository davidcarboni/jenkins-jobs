package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * Handles jobs in the Maven Build category.
 */
public class MavenJobs {

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
        return ResourceUtils.getXml("/config-maven.xml");
    }

    private static void setGitUrl(String gitUrl, Document template) throws IOException {
        Xml.setTextValue(template, "//hudson.plugins.git.UserRemoteConfig/url", gitUrl);
    }

    private static void setBranch(String branch, Document template) throws IOException {
        Xml.setTextValue(template, "//hudson.plugins.git.BranchSpec/name", branch);
    }
}
