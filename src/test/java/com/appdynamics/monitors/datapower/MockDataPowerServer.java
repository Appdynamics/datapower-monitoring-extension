package com.appdynamics.monitors.datapower;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by abey.tom on 11/9/14.
 */
public class MockDataPowerServer {
    public static final Logger logger = LoggerFactory.getLogger(MockDataPowerServer.class);
    private static Server server;

    public static void main(String[] args) throws Exception {
        startServer();
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
        SocketConnector connector = new SocketConnector();
        int port = 8654;
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
            String inXml = IOUtils.toString(request.getInputStream(), "UTF-8");
            Matcher matcher = Pattern.compile("class=\"(\\w+)\"").matcher(inXml);

            if (matcher.find()) {
                String operation = matcher.group(1);
                InputStream in = getClass().getResourceAsStream("/output/" + operation + ".xml");
                if (in != null) {
                    ServletOutputStream out = response.getOutputStream();
                    IOUtils.write(IOUtils.toByteArray(in), out);
                    in.close();
                    out.flush();
                    out.close();
                } else {
                    logger.error("Cannot find the response file for the operation {}", operation);
                }
            } else {
                logger.error("Cannot find the operation from the input {}", inXml);
            }
            response.setStatus(200);
        }
    }
}
