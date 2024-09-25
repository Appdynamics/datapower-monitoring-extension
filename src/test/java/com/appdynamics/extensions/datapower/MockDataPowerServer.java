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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
        server.setConnectors(new Connector[]{connector});
        server.setHandler(new DelegateHandler());
        logger.info("Starting the server on {}", port);
        server.start();
    }

    public static void startServerSSL() throws Exception {
        SslContextFactory factory = new SslContextFactory();
//        factory.setProtocol("TLSv1.2");
//        factory.setIncludeProtocols("TLSv1.2");
//        factory.setExcludeProtocols("TLSv1.1","TLSv1.0");
        factory.setKeyStoreResource(Resource.newClassPathResource("/keystore/keystore.jks"));
        factory.setKeyStorePassword("changeit");
        if (server != null) {
            server.stop();
        }
        int port = 5550;
        server = new Server(port);
        ServerConnector connector = new ServerConnector(server, factory);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});
        server.setHandler(new DelegateHandler());
        logger.info("Starting the server on {}", port);
        server.start();
    }



    private static class DelegateHandler extends AbstractHandler {

        private DelegateHandler() {
        }

        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {
            String authorization = request.getHeader("Authorization");
            if (!Strings.isNullOrEmpty(authorization)) {
                logger.info("The Auth Header is {}", authorization);
                String userPass = new String(Base64.decodeBase64(authorization.replace("Basic ", "")));
                if ("user:welcome".equals(userPass)) {
                    handle(target, request, response);
                } else {
                    logger.info("InCorrect User and Password");
                    response.setStatus(401);
                }
            } else {
                response.setStatus(401);
                logger.info("Auth not present, requesting authentication");
                response.setHeader("WWW-Authenticate", "Basic realm=\"Mock Test\"");
            }
            baseRequest.setHandled(true);

        }

        private void handle(String target, HttpServletRequest request, HttpServletResponse response) throws IOException {
            logger.info("Serving a connection {}", target);
            String inXml = IOUtils.toString(request.getInputStream(), "UTF-8");
            Matcher matcher = Pattern.compile("class=\"(\\w+)\"").matcher(inXml);
            if (matcher.find()) {
                String operation = matcher.group(1);
                String file;
                if(operation.equals("DomainStatus")){
                    file = "/output/" + operation + ".xml";
                } else{
                    file = "/output/BulkResponse.xml";
                }
                InputStream in = getClass().getResourceAsStream(file);
                if (in != null) {
                    ServletOutputStream out = response.getOutputStream();
                    IOUtils.write(IOUtils.toByteArray(in), out);
                    in.close();
                    out.flush();
                    out.close();
                } else {
                    logger.error("Cannot find the response file for the operation {}", file);
                }
            } else {
                response.setStatus(404);
                PrintWriter writer = response.getWriter();
                writer.close();
                logger.error("Cannot find the operation from the input {}", inXml);
            }
        }
    }
}
