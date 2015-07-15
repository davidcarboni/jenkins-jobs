package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * Checks the behaviour of {@link MavenJobs}
 */
public class MavenJobsTest {

    @Test
    public void shouldSetUrl() throws IOException {

        // Given
        String gitUrl = "Huzzah!";

        // When
        Document document = MavenJobs.forRepo(gitUrl);

        // Then
        Assert.assertEquals(gitUrl, getGitUrl(document));
    }

    @Test
    public void shouldSetUrlAndBranch() throws IOException {

        // Given
        String gitUrl = "Cryptolite";
        String branch = "Pooma";

        // When
        Document document = MavenJobs.forRepo(gitUrl, branch);

        // Then
        Assert.assertEquals(gitUrl, getGitUrl(document));
        Assert.assertEquals(branch, getBranch(document));
    }


    private static Document getTemplate() throws IOException {
        return ResourceUtils.getXml("/config-maven.xml");
    }

    private static String getGitUrl(Document template) throws IOException {
        return Xml.getTextValue(template, "//hudson.plugins.git.UserRemoteConfig/url");
    }

    private static String getBranch(Document template) throws IOException {
        return Xml.getTextValue(template, "//hudson.plugins.git.BranchSpec/name");
    }
}