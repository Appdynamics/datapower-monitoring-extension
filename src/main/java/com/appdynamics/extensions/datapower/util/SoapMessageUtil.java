/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower.util;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds DataPower XML Management Interface SOAP requests and extracts results from responses.
 *
 * The request envelope is built as a plain string. Earlier versions used SAAJ (javax.xml.soap),
 * but the SAAJ implementation was removed from the JDK in Java 11 (JEP 320), so the constructor
 * failed on Machine Agents running a modern JRE with:
 * "Cannot invoke SAAJMetaFactory.newMessageFactory(String) because SAAJMetaFactory.getInstance() is null".
 * The envelope is static apart from the escaped domain and status-class values, so no SOAP
 * library is needed. Responses were never parsed with SAAJ and are unchanged.
 */
public class SoapMessageUtil {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(SoapMessageUtil.class);

    static final String SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String DP_MGMT_NS = "http://www.datapower.com/schemas/management";

    private static final String ENVELOPE_START =
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"" + SOAP_ENV_NS + "\">"
                    + "<SOAP-ENV:Header/>"
                    + "<SOAP-ENV:Body xmlns:dp=\"" + DP_MGMT_NS + "\">";
    private static final String ENVELOPE_END = "</SOAP-ENV:Body></SOAP-ENV:Envelope>";

    public SoapMessageUtil() {
    }

    public String createSoapMessage(String request, String domain) {
        try {
            return buildEnvelope(Collections.singletonList(request), domain);
        } catch (Exception e) {
            throw new SoapMessageException("Cannot create a SOAP message for the request " + request, e);
        }
    }

    public String createSoapMessage(Collection<String> operations, String domain) {
        try {
            return buildEnvelope(operations, domain);
        } catch (Exception e) {
            throw new SoapMessageException("Cannot create a SOAP message for the request " + operations, e);
        }
    }

    private String buildEnvelope(Collection<String> operations, String domain) {
        StringBuilder sb = new StringBuilder(ENVELOPE_START);
        for (String operation : operations) {
            addRequest(operation, domain, sb);
        }
        return sb.append(ENVELOPE_END).toString();
    }

    private void addRequest(String request, String domain, StringBuilder sb) {
        if (request == null) {
            throw new IllegalArgumentException("The status class (operation) cannot be null");
        }
        sb.append("<dp:request");
        if (domain != null) {
            sb.append(" domain=\"").append(escapeXmlAttribute(domain)).append('"');
        }
        sb.append("><dp:get-status class=\"").append(escapeXmlAttribute(request)).append("\"/></dp:request>");
    }

    static String escapeXmlAttribute(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&apos;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    public Xml[] getSoapResponseBody(InputStream inputStream, String operation) {
        Xml xml = new Xml(inputStream);
        if (logger.isDebugEnabled()) {
            logger.debug("The response for the operation {} is {}", operation, xml.toString());
        }
        return getSoapResponseBody(xml, operation);
    }

    /**
     * The XML response from teh server is not a valid xml. Hence the regex workaround.
     *
     * @param xmlStr
     * @param operations
     * @return
     */
    public Map<String, Xml[]> getSoapResponseBody(String xmlStr, Collection<String> operations) {
        logger.debug("The response for the operations [{}] are {}", operations, xmlStr);
        Map<String, Xml[]> xmlMap = new HashMap<String, Xml[]>();
        for (String operation : operations) {
            String content = getResponseContent(operation, xmlStr);
            logger.trace("The response string from the {} is {}", operation, content);
            if (content != null) {
                String response = "<response>" + content + "</response>";
                try {
                    Xml xml = new Xml(response);
                    Xml[] xmls = getSoapResponseBody(xml, operation);
                    xmlMap.put(operation, xmls);
                } catch (Exception e) {
                    logger.error("Error while parsing operation=[" + operation + "] and response " + response, e);
                }
            }
        }
        return xmlMap;
    }

    private String getResponseContent(String operation, String xmlStr) {
        Integer start = Integer.valueOf(xmlStr.indexOf("<" + operation+" "));
        if (start != -1) {
            String xmlClose = "</" + operation + ">";
            Integer end = Integer.valueOf(xmlStr.lastIndexOf(xmlClose));
            if (end != -1 && end > start) {
                logger.trace("The start is {} and end is {}", start, end);
                return xmlStr.substring(start, end + xmlClose.length());
            } else {
                logger.debug("Operation[{}], start element found at {}, but end index is {}", operation, start, end);
                return null;
            }
        } else {
            return null;
        }
    }

    private Xml[] getSoapResponseBody(Xml xml, String operation) {
        NodeList nodes = xml.getNode("//" + operation);
        if (nodes != null && nodes.getLength() > 0) {
            Xml[] xmls = new Xml[nodes.getLength()];
            for (int i = 0; i < nodes.getLength(); i++) {
                xmls[i] = Xml.from(nodes.item(i));
            }
            return xmls;
        } else {
            logger.error("The {} returned null from the {}", operation, xml.toString());
        }
        return null;
    }

    public static class SoapMessageException extends RuntimeException {
        public SoapMessageException(String message, Throwable cause) {
            super(message, cause);
        }
    }


}
