package com.appdynamics.monitors.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a simple wrapper around the java xml implementation to lookup the elements with simple xpath expressions.
 * <p/>
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 2:33 PM
 */
public class Xml {
    public static final Logger logger = LoggerFactory.getLogger(Xml.class);
    private Node node;
    private Document document;

    public Xml(String xml) {
        try {
            document = getDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (SAXException e) {
            logger.error("", e);
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    public Xml(InputStream in) {
        try {
            document = getDocumentBuilder().parse(in);
        } catch (SAXException e) {
            logger.error("", e);
        } catch (IOException e) {
            logger.error("", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public NodeList getElementsByTagName(String tagName) {
        Node source = getSource();
        if (source instanceof Document) {
            return ((Document) source).getElementsByTagName(tagName);
        }
        return null;
    }

    private DocumentBuilder getDocumentBuilder() {
        DocumentBuilder documentBuilder = LocalXmlObjectFactory.getDocumentBuilder();
        if (documentBuilder != null) {
            return documentBuilder;
        } else {
            throw new XmlException("There was a problem in initializing the DocumentBuilder. Please see the log for details");
        }
    }

    private static XPath getXpath() {
        XPath xPath = LocalXmlObjectFactory.getXPath();
        if (xPath != null) {
            return xPath;
        } else {
            throw new XmlException("XPath cannot be initialized. Please check the logs for details.");
        }
    }

    public Xml(Node node) {
        this.node = node;
    }

    public static Xml fromString(String xml) {
        return new Xml(xml);
    }

    public static Xml from(Node node) {
        return new Xml(node);
    }

    public String getText(String xpathStr) {
        return (String) evaluate(xpathStr, XPathConstants.STRING);
    }

    public Xml getXmlFromXpath(String xpathStr) {
        Node node = (Node) evaluate(xpathStr, XPathConstants.NODE);
        if (node != null) {
            return new Xml(node);
        }
        return null;
    }

    public Object evaluate(String xpathStr, QName qName) {
        if (xpathStr != null) {
            xpathStr = xpathStr.trim();
        }
        try {
            XPathExpression expression = getXpath().compile(xpathStr);
            return expression.evaluate(getSource(), qName);
        } catch (XPathExpressionException e) {
            throw new XmlException("The xpath expression " + xpathStr + " doesn't seem to be valid", e);
        }
    }

    public Node getSource() {
        if (document != null) {
            return document;
        } else if (node != null) {
            return node;
        }
        return null;
    }

    public NodeList getNode(String xpathStr) {
        return getNodes(xpathStr, getSource());
    }

    public Node getFirstNode(String xpathStr) {
        NodeList nodes = getNodes(xpathStr, getSource());
        if (nodes != null && nodes.getLength() > 0) {
            return nodes.item(0);
        }
        return null;
    }

    public static NodeList getNodes(String xpathStr, Node node) {
        try {
            XPathExpression expression = getXpath().compile(xpathStr);
            return (NodeList) expression.evaluate(node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new XmlException("The xpath expression " + xpathStr + " doesn't seem to be valid", e);
        }
    }

    public static Node getFirstDescendant(Node node, String nodeName) {
        if (node != null) {
            NodeList childNodes = node.getChildNodes();
            if (childNodes != null) {
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if (child.getNodeName().equals(nodeName)) {
                        return child;
                    } else {
                        Node descendant = getFirstDescendant(child, nodeName);
                        if (descendant != null) {
                            return descendant;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<Node> getDescendants(Node node, String nodeName) {
        if (node != null) {
            List<Node> nodes = new ArrayList<Node>();
            getDescendants(node, nodeName, nodes);
            return nodes;
        }
        return null;
    }

    private static void getDescendants(Node node, String nodeName, List<Node> nodes) {
        NodeList childNodes = node.getChildNodes();
        if (childNodes != null) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                if (child.getNodeName().equals(nodeName)) {
                    nodes.add(child);
                } else {
                    getDescendants(child, nodeName, nodes);
                }
            }
        }
    }

    public static Node getFirstChild(Node node, String nodeName) {
        if (node != null) {
            NodeList childNodes = node.getChildNodes();
            if (childNodes != null) {
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if (child.getNodeName().equals(nodeName)) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    public Xml getParent() {
        return new Xml(node.getParentNode());
    }


    public static class XmlException extends RuntimeException {
        public XmlException(String message) {
            super(message);
        }

        public XmlException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    @Override
    public String toString() {
        Transformer transformer = LocalXmlObjectFactory.getTransformer();
        if (transformer != null) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            try {
                transformer.transform(new DOMSource(getSource()), new StreamResult(writer));
            } catch (TransformerException e) {
                logger.error("Exception while transforming the xml", e);
            }
            return writer.getBuffer().toString();
        } else {
            throw new RuntimeException("Cannot transform the xml since the XML Transformer cannot be created");
        }
    }
}
