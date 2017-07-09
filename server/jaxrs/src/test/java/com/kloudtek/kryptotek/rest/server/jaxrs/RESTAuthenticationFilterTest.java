/*
 * Copyright (c) 2016 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.server.jaxrs;

import com.kloudtek.kryptotek.rest.server.TestHelper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.logging.Logger;

public class RESTAuthenticationFilterTest {
    private static final Logger logger = Logger.getLogger(RESTAuthenticationFilterTest.class.getName());
    private String url;
    private Server server;
    private TestHelper testHelper = new TestHelper(url);

    @BeforeMethod
    public void init() throws Exception {
        server = new Server(0);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder h = new ServletHolder(new HttpServletDispatcher());
        h.setInitParameter("javax.ws.rs.Application", TestApp.class.getName());
        context.addServlet(h, "/*");
        server.setHandler(context);
        server.start();
        url = "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        testHelper = new TestHelper(url);
    }

    @AfterClass
    public void close() throws Exception {
        server.stop();
    }

    @Test
    public void testValidHmac() throws IOException, InvalidKeyException {
        testHelper.testValidHmac();
    }

    @Test
    public void testExpiredHmac() throws IOException, InvalidKeyException {
        testHelper.testExpiredHmac();
    }

    @Test
    public void testInvalidHmac() throws Exception {
        testHelper.testInvalidHmac();
    }

    @Test
    public void testException() throws Exception {
        testHelper.testException(true);
    }
}