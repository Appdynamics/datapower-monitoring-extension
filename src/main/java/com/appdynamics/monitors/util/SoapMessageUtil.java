package com.appdynamics.monitors.util;

import com.appdynamics.extensions.xml.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 3:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoapMessageUtil {
    public static final Logger logger = LoggerFactory.getLogger(SoapMessageUtil.class);

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
            SOAPElement req = soapBody.addChildElement("request", "dp");
            if (domain != null) {
                req.addAttribute(new QName("domain"), domain);
            }
            SOAPElement status = req.addChildElement("get-status", "dp");
            status.setAttribute("class", request);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            soapMessage.writeTo(out);
            return new String(out.toByteArray());
        } catch (Exception e) {
            throw new SoapMessageException("Cannot create a SOAP message for the request " + request, e);
        }
    }

    public Xml[] getSoapResponseBody(InputStream inputStream, String operation) {
        Xml xml = new Xml(inputStream);
        if (logger.isDebugEnabled()) {
            logger.debug("The response for the operation {} is {}", operation, xml.toString());
        }
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
