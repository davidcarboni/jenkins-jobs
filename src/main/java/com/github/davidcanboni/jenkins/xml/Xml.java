package com.github.davidcanboni.jenkins.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 09/07/2015.
 */
public class Xml {

    static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    public static String toString(Node n) {
        try {
            StringWriter writer = new StringWriter();
            transformer().transform(new DOMSource(n), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            return output;
        } catch (TransformerException e) {
            throw new RuntimeException("Error converting XML to String", e);
        }
    }

    public static Path toFile(Node n) throws IOException {
        try {
            Path output = Files.createTempFile(n.getNodeName(), "xml");
            Writer writer = new OutputStreamWriter(Files.newOutputStream(output));
            transformer().transform(new DOMSource(n), new StreamResult(writer));
            return output;
        } catch (TransformerException e) {
            throw new RuntimeException("Error converting XML to String", e);
        }
    }

    public static Document fromFile(Path file) throws IOException {

        Document document;

        try (InputStream input = Files.newInputStream(file)) {

            // Parse the stream:
            // Adapted from:
            // http://www.java-samples.com/showtutorial.php?tutorialid=152
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilderFactory.newDocumentBuilder().parse(input);
            document.setXmlStandalone(true);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IOException("Error reading XML from: " + file, e);
        }

        return document;
    }

    public static Node getNode(Document document, String xpathExpression) {

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression xPathExpression = xPath.compile(xpathExpression);
            return (Node) xPathExpression.evaluate(document, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Invalid xpath expression: " + xpathExpression);
        } catch (NullPointerException e) {
            throw new RuntimeException("No node found for: " + xpathExpression);
        }
    }

    public static void setTextValue(Document document, String xpathExpression, String value) {
        Node node = getNode(document, xpathExpression);
        node.setTextContent(value);
    }

    public static String getTextValue(Document document, String xpathExpression) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression xPathExpression = xPath.compile(xpathExpression);
            Node node = (Node) xPathExpression.evaluate(document, XPathConstants.NODE);
            return node.getTextContent();
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Invalid xpath expression: " + xpathExpression);
        } catch (NullPointerException e) {
            throw new RuntimeException("No node found for: " + xpathExpression);
        }
    }

    private static Transformer transformer() throws TransformerConfigurationException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        return transformer;
    }
}
