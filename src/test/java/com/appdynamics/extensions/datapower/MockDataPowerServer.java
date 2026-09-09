/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.google.common.base.Strings;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by abey.tom on 11/9/14.
 */
public class MockDataPowerServer {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MockDataPowerServer.class);
    private static Server server;

    public static void main(String[] args) throws Exception {
        startServerSSL();
    }

    public static void startServerAsync() {
        try {
            startServer();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void stopServer() {
        if (server != null) {
            try {
                logger.info("Stopping the Mock DataPower Server");
                server.stop();
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }


    private static void startServer() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        int port = 5550;
        connector.setPort(port);
        server.setConnectors(new org.eclipse.jetty.server.Connector[]{connector});
        server.setHandler(new DelegateHandler());
        logger.info("Starting the server on {}", port);
        server.start();
    }

    public static void startServerSSL() throws Exception {
        if (server != null) {
            server.stop();
        }
        int port = 5550;
        server = new Server();
        SslContextFactory.Server factory = new SslContextFactory.Server();
        Resource keyStoreResource = ResourceFactory.of(server).newClassPathResource("/keystore/keystore.jks");
        factory.setKeyStoreResource(keyStoreResource);
        factory.setKeyStorePassword("changeit");
        // The test cert's CN (appdynamics.com) won't match the "localhost" SNI a real client sends,
        // so relax Jetty's SNI enforcement for this local test-only server.
        factory.setSniRequired(false);
        HttpConfiguration httpConfig = new HttpConfiguration();
        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniHostCheck(false);
        httpConfig.addCustomizer(customizer);
        ServerConnector connector = new ServerConnector(server,
                new SslConnectionFactory(factory, "http/1.1"),
                new HttpConnectionFactory(httpConfig));
        connector.setPort(port);
        server.setConnectors(new org.eclipse.jetty.server.Connector[]{connector});
        server.setHandler(new DelegateHandler());
        logger.info("Starting the server on {}", port);
        server.start();
    }



    private static class DelegateHandler extends Handler.Abstract {

        private DelegateHandler() {
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            String target = Request.getPathInContext(request);
            String authorization = request.getHeaders().get("Authorization");
            if (!Strings.isNullOrEmpty(authorization)) {
                logger.info("The Auth Header is {}", authorization);
                String userPass = new String(Base64.decodeBase64(authorization.replace("Basic ", "")));
                if ("user:welcome".equals(userPass)) {
                    handle(target, request, response, callback);
                } else {
                    logger.info("InCorrect User and Password");
                    response.setStatus(401);
                    callback.succeeded();
                }
            } else {
                response.setStatus(401);
                logger.info("Auth not present, requesting authentication");
                response.getHeaders().put("WWW-Authenticate", "Basic realm=\"Mock Test\"");
                callback.succeeded();
            }
            return true;
        }

        private void handle(String target, Request request, Response response, Callback callback) throws Exception {
            logger.info("Serving a connection {}", target);
            String inXml = IOUtils.toString(Request.asInputStream(request), "UTF-8");
            Matcher matcher = Pattern.compile("class=\"(\\w+)\"").matcher(inXml);
            if (matcher.find()) {
                String operation = matcher.group(1);
                String file;
                if (operation.equals("DomainStatus")) {
                    file = "/output/" + operation + ".xml";
                } else {
                    file = "/output/BulkResponse.xml";
                }
                InputStream in = getClass().getResourceAsStream(file);
                if (in != null) {
                    byte[] bytes = IOUtils.toByteArray(in);
                    in.close();
                    response.write(true, ByteBuffer.wrap(bytes), callback);
                } else {
                    logger.error("Cannot find the response file for the operation {}", file);
                    response.setStatus(500);
                    callback.succeeded();
                }
            } else {
                response.setStatus(404);
                callback.succeeded();
                logger.error("Cannot find the operation from the input {}", inXml);
            }
        }
    }
}
