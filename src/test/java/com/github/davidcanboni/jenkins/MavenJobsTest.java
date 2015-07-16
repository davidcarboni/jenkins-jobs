package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.values.Environment;
import com.github.davidcanboni.jenkins.values.GitRepo;
import com.github.davidcanboni.jenkins.xml.Xml;
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
        GitRepo repo = GitRepo.babbage;
        Environment environment = Environment.live;

        // When
        Document document = MavenJobs.forRepo(repo, environment);

        // Then
        Assert.assertEquals(repo.url.toString(), getGitUrl(document));
    }

    @Test
    public void shouldSetBranch() throws IOException {

        // Given
        GitRepo repo = GitRepo.florence;
        Environment environment = Environment.staging;

        // When
        Document document = MavenJobs.forRepo(repo, environment);

        // Then
        Assert.assertEquals("*/" + environment.name(), getBranch(document));
    }

    @Test
    public void shouldSetContainerJob() throws IOException {

        // Given
        GitRepo repo = GitRepo.zebedee;
        Environment environment = Environment.develop;

        // When
        Document document = MavenJobs.forRepo(repo, environment);

        // Then
        Assert.assertEquals(ContainerJobs.jobName(repo, environment), getContainerJob(document));
    }

    private static String getGitUrl(Document template) throws IOException {
        return Xml.getTextValue(template, "//hudson.plugins.git.UserRemoteConfig/url");
    }

    private static String getBranch(Document template) throws IOException {
        return Xml.getTextValue(template, "//hudson.plugins.git.BranchSpec/name");
    }

    private static String getContainerJob(Document template) throws IOException {
        return Xml.getTextValue(template, "//publishers/hudson.tasks.BuildTrigger/childProjects");
    }
}