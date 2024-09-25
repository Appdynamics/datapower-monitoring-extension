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

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 3:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoapMessageUtil {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(SoapMessageUtil.class);

    private final MessageFactory messageFactory;

    public SoapMessageUtil() {
        try {
            messageFactory = MessageFactory.newInstance();
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
    }

    public String createSoapMessage(String request, String domain) {
        try {
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPBody soapBody = envelope.getBody();
            soapBody.addNamespaceDeclaration("dp", "http://www.datapower.com/schemas/management");
            addRequest(request, domain, soapBody);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            soapMessage.writeTo(out);
            return new String(out.toByteArray());
        } catch (Exception e) {
            throw new SoapMessageException("Cannot create a SOAP message for the request " + request, e);
        }
    }

    public String createSoapMessage(Collection<String> operations, String domain) {
        try {
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPBody soapBody = envelope.getBody();
            soapBody.addNamespaceDeclaration("dp", "http://www.datapower.com/schemas/management");
            for (String operation : operations) {
                addRequest(operation, domain, soapBody);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            soapMessage.writeTo(out);
            return new String(out.toByteArray());
        } catch (Exception e) {
            throw new SoapMessageException("Cannot create a SOAP message for the request " + operations, e);
        }
    }

    private void addRequest(String request, String domain, SOAPBody soapBody) throws SOAPException {
        SOAPElement req = soapBody.addChildElement("request", "dp");
        if (domain != null) {
            req.addAttribute(new QName("domain"), domain);
        }
        SOAPElement status = req.addChildElement("get-status", "dp");
        status.setAttribute("class", request);
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
