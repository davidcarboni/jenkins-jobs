package com.github.davidcanboni.jenkins;

import com.github.davidcanboni.jenkins.xml.Xml;
import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.http.Endpoint;
import com.github.onsdigital.http.Http;
import com.github.onsdigital.http.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles jobs in the Monitoring category.
 */
public class MonitorJobs  {

    static String xpath = "//hudson.model.StringParameterDefinition/defaultValue";

    public static Document setUrl(URL url) throws IOException, URISyntaxException {
        Document document = ResourceUtils.getXml(Templates.configMonitor);
        setUrl(url, document);
        return document;
    }

    public static void setUrl(URL url, Document document) throws IOException, URISyntaxException {
        Xml.setTextValue(document, xpath, url.toString());
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node node;
        try {
            XPathExpression xPathExpression = xPath.compile(xpath);
            node = (Node) xPathExpression.evaluate(document, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Invalid xpath expression: " + xpath);
        }
        node.setTextContent(url.toString());
    }


    public static void create(String name, URL url) throws IOException, URISyntaxException {

        String host = url.getHost()+url.getPath();
        String password = StringUtils.isNotBlank(url.getUserInfo()) ? " (with password)" : "";

        if (StringUtils.isNotBlank(name)) {
            try (Http http = new Http()) {

                http.addHeader("Content-Type", "application/xml");

                String jobName = "Monitor " + name;

                if (!Jobs.exists(jobName)) {

                    System.out.println("Creating job to monitor " + name + " - " + host + password );

                    // Set the URL and create:
                    Document config = setUrl(url);
                    create(jobName, config, http);

                } else {

                    System.out.println("Updating job to monitor " + name + " - " + host + password );
                    Endpoint endpoint = new Endpoint(Jobs.jenkins, "/job/" + jobName + "/config.xml");
                    Response<Path> xml = http.get(endpoint);
                    if (xml.statusLine.getStatusCode() != 200)
                        throw new RuntimeException("Error reading configuration for job " + jobName + ": " + xml.statusLine.getReasonPhrase());
                    Document config = Xml.fromFile(xml.body);

                    // Set the URL and update:
                    setUrl(url, config);
                    update(jobName, config, http, endpoint);

                }

            }
        }
    }

    private static void create(String jobName, Document config, Http http) throws IOException {


        // Post the config XML to create the job
        Endpoint endpoint = Jobs.createItem.setParameter("name", jobName);
        Response<String> create = http.post(endpoint, config, String.class);
        if (create.statusLine.getStatusCode() != 200) {
            System.out.println(create.body);
            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + create.statusLine.getReasonPhrase());
        }
    }

    private static void update(String jobName, Document config, Http http, Endpoint endpoint) throws IOException {

        // Post the config XML to update the job
        Response<String> create = http.post(endpoint, config, String.class);
        if (create.statusLine.getStatusCode() != 200) {
            System.out.println(create.body);
            throw new RuntimeException("Error setting configuration for job " + jobName + ": " + create.statusLine.getReasonPhrase());
        }
    }


    public static void main(String[] args) throws IOException, URISyntaxException {
        Map<String, URL> monitors = new HashMap<>();

        for (Environment environment : Environment.values()) {

            monitors.put(WordUtils.capitalize(environment.name())+" Website", environment.website());
            monitors.put(WordUtils.capitalize(environment.name())+" Publishing", environment.publishing());
        }

        monitors.put("Build Jenkins", Environment.jenkins());
        monitors.put("Build Registry", Environment.registry());
        monitors.put("Build Nexus", Environment.nexus());

        for (Map.Entry<String, URL> entry : monitors.entrySet()) {
            String name = entry.getKey();
            URL url = entry.getValue();
            create(name, url);
        }
    }
}
