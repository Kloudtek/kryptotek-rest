package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.rest.server.TestHelper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.EnumSet;

/**
 * Created by yannick on 6/24/17.
 */
public class RestSpringAuthenticationFilterTest {
    private Server server;
    private TestHelper testHelper;

    @BeforeClass
    public void init() throws Exception {
        server = new Server(0);
        AnnotationConfigWebApplicationContext springCtx = new AnnotationConfigWebApplicationContext();
        springCtx.setConfigLocation(Config.class.getName());
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addFilter(new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")), "/*", EnumSet.allOf(DispatcherType.class));
        contextHandler.addServlet(new ServletHolder(new DispatcherServlet(springCtx)), "/*");
        contextHandler.addEventListener(new ContextLoaderListener(springCtx));
        server.setHandler(contextHandler);
        server.start();
        testHelper = new TestHelper("http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort());
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
        testHelper.testException(false);
    }
}