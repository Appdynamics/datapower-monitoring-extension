package com.appdynamics.extensions.datapower.util;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * Covers the SAAJ-free SOAP envelope builder. SAAJ was removed from the JDK in Java 11,
 * so these tests also guard against reintroducing a javax.xml.soap dependency.
 */
public class SoapMessageUtilTest {

    private static final String SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String DP_NS = "http://www.datapower.com/schemas/management";

    private final SoapMessageUtil util = new SoapMessageUtil();

    @Test
    public void constructorWorksWithoutSaaj() {
        Assert.assertNotNull(new SoapMessageUtil());
    }

    @Test
    public void singleRequestMatchesDocumentedEnvelope() {
        String expected = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<SOAP-ENV:Header/>"
                + "<SOAP-ENV:Body xmlns:dp=\"http://www.datapower.com/schemas/management\">"
                + "<dp:request domain=\"default\"><dp:get-status class=\"HTTPMeanTransactionTime\"/></dp:request>"
                + "</SOAP-ENV:Body></SOAP-ENV:Envelope>";
        Assert.assertEquals(expected, util.createSoapMessage("HTTPMeanTransactionTime", "default"));
    }

    @Test
    public void bulkRequestContainsOneRequestPerOperationInOrder() throws Exception {
        Document doc = parse(util.createSoapMessage(Arrays.asList("CPUUsage", "MemoryStatus", "SystemUsage"), "domain1"));
        Element root = doc.getDocumentElement();
        Assert.assertEquals(SOAP_ENV_NS, root.getNamespaceURI());
        Assert.assertEquals("Envelope", root.getLocalName());

        NodeList requests = doc.getElementsByTagNameNS(DP_NS, "request");
        NodeList statuses = doc.getElementsByTagNameNS(DP_NS, "get-status");
        Assert.assertEquals(3, requests.getLength());
        Assert.assertEquals(3, statuses.getLength());
        Assert.assertEquals("domain1", ((Element) requests.item(0)).getAttribute("domain"));
        Assert.assertEquals("CPUUsage", ((Element) statuses.item(0)).getAttribute("class"));
        Assert.assertEquals("SystemUsage", ((Element) statuses.item(2)).getAttribute("class"));
    }

    @Test
    public void nullDomainOmitsDomainAttribute() throws Exception {
        Document doc = parse(util.createSoapMessage("CPUUsage", null));
        Element request = (Element) doc.getElementsByTagNameNS(DP_NS, "request").item(0);
        Assert.assertFalse(request.hasAttribute("domain"));
    }

    @Test
    public void specialCharactersAreEscaped() throws Exception {
        String domain = "a&b<c>\"d'e";
        Document doc = parse(util.createSoapMessage("CPUUsage", domain));
        Element request = (Element) doc.getElementsByTagNameNS(DP_NS, "request").item(0);
        Assert.assertEquals(domain, request.getAttribute("domain"));
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }
}
