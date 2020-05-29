package com.appdynamics.extensions.datapower.util;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.slf4j.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 * The "DocumentBuilderFactory.newInstance()" and "XPathFactory.newInstance()" are extremely expensive operations
 * and at the same time it is not ThreadSafe. So the only option is to create a ThreadLocal and Pray that these will NOT be invoked
 * through a new thread everytime.
 * <p/>
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 5/2/14
 * Time: 9:18 AM
 */
public class LocalXmlObjectFactory {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(LocalXmlObjectFactory.class);

    private static ThreadLocal<DocumentBuilder> builderThreadLocal = new ThreadLocal<DocumentBuilder>() {
        @Override
        protected DocumentBuilder initialValue() {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                logger.error("Error while creating the Document Builder", e);
                return null;
            }
        }
    };

    private static ThreadLocal<Transformer> transformerThreadLocal = new ThreadLocal<Transformer>() {
        @Override
        protected Transformer initialValue() {
            TransformerFactory tf = TransformerFactory.newInstance();
            try {
                return tf.newTransformer();
            } catch (TransformerConfigurationException e) {
                logger.error("Error while building the transformer factory", e);
                return null;
            }
        }
    };

    private static ThreadLocal<XPath> xPathThreadLocal = new ThreadLocal<XPath>() {
        @Override
        protected XPath initialValue() {
            XPathFactory xpathFactory = XPathFactory.newInstance();
            return xpathFactory.newXPath();
        }
    };

    public static DocumentBuilder getDocumentBuilder() {
        ThreadLocal<DocumentBuilder> local = builderThreadLocal;
        DocumentBuilder documentBuilder = local.get();
        if (documentBuilder != null) {
            documentBuilder.reset();
            return documentBuilder;
        } else {
            return null;
        }
    }

    public static XPath getXPath() {
        ThreadLocal<XPath> local = xPathThreadLocal;
        XPath xPath = local.get();
        if (xPath != null) {
            xPath.reset();
            return xPath;
        } else {
            return null;
        }
    }

    public static Transformer getTransformer() {
        Transformer transformer = transformerThreadLocal.get();
        if (transformer != null) {
            transformer.reset();
            return transformer;
        } else{
            return null;
        }
    }
}
