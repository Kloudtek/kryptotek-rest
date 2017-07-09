/*
 * Copyright (c) 2016 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.client.jaxrs;

import com.kloudtek.kryptotek.CryptoEngine;
import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.jce.JCECryptoEngine;
import com.kloudtek.kryptotek.key.HMACKey;
import com.kloudtek.kryptotek.rest.RESTRequestSigner;
import com.kloudtek.kryptotek.rest.RESTResponseSigner;
import com.kloudtek.util.StringUtils;
import com.kloudtek.util.TimeUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.client.ClientConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import static com.kloudtek.kryptotek.rest.RESTRequestSigner.*;
import static com.kloudtek.util.StringUtils.utf8;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class JAXRSClientTest {
    public static final String IDENTITY = "user";
    public static final String TEST_SERVLET_PATH = "/afdsfdsafsda";
    public static final String TEST_SERVLET_PATH_FULL = "/test/afdsfdsafsda?a=b";
    private static final CryptoEngine cryptoEngine = new JCECryptoEngine();
    private static final HMACKey HMAC_KEY = cryptoEngine.generateHMACKey(DigestAlgorithm.SHA256);
    private static final byte[] DATA = "safdfsa893wfjsafj893q2fjidwaqjf8913rjo14879fsdkjdl".getBytes();
    private static final byte[] DATA_RESP = "fs7fyw3jkfh8sjwqafliu8rujlsajf".getBytes();
    public static final String TIME_PATH = "/time";
    public static final String TIME_PATH_FULL = "/test/time";
    private CloseableHttpClient httpClient;
    private Server server;
    private String url;
    private TestServlet testServlet;
    private TimeServlet timeServlet;

    @BeforeMethod
    public void setup() throws Exception {
        server = new Server(0);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/test/");
        testServlet = new TestServlet();
        timeServlet = new TimeServlet();
        context.addServlet(new ServletHolder(testServlet), TEST_SERVLET_PATH);
        context.addServlet(new ServletHolder(timeServlet), TIME_PATH);
        server.setHandler(context);
        server.start();
        url = "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    @AfterMethod
    public void cleanup() throws Exception {
        httpClient.close();
        server.stop();
    }

    @Test
    public void testSimple() throws Exception {
        Client client = ClientBuilder.newClient(new ClientConfig(RESTAuthenticationFilter.class));
        final WebTarget target = client.target(url);
        final RESTInterface proxy = ((ResteasyWebTarget) target).proxy(RESTInterface.class);
        final String data = proxy.doStuff("b", DATA);
    }

    public class TestServlet extends HttpServlet {
        private static final long serialVersionUID = -2507734802640341400L;
        private Date timestamp;
        private long timeSlip = 0;
        private boolean badReply;

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            super.service(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                String nonce = req.getHeader(HEADER_NONCE);
                String timestampStr = req.getHeader(HEADER_TIMESTAMP);
                assertNotNull(nonce);
                assertNotNull(timestampStr);
                timestamp = TimeUtils.parseISOUTCDateTime(timestampStr);
                long expectedTimestamp = System.currentTimeMillis() + timeSlip;
                long diff = timestamp.getTime() - expectedTimestamp;
                if (diff > 2000L || diff < -2000L) {
                    fail("Time difference too large: " + diff);
                }
                RESTRequestSigner requestSigner = new RESTRequestSigner("POST", TEST_SERVLET_PATH_FULL, nonce, timestampStr, "user");
                requestSigner.setContent(DATA);
                String sig = req.getHeader(HEADER_SIGNATURE);
                cryptoEngine.verifySignature(HMAC_KEY, requestSigner.getDataToSign(), StringUtils.base64Decode(sig));
                RESTResponseSigner responseSigner = new RESTResponseSigner(nonce, sig, 200);
                responseSigner.setContent(badReply ? "fdsafads".getBytes() : DATA_RESP);
                resp.setHeader(HEADER_SIGNATURE, StringUtils.base64Encode(cryptoEngine.sign(HMAC_KEY, responseSigner.getDataToSign())));
                resp.getOutputStream().write(DATA_RESP);
            } catch (Exception e) {
                fail(e.getMessage(), e);
            }
        }

        private void addSigData(ByteArrayOutputStream dataToSign, String... data) throws IOException {
            for (String d : data) {
                dataToSign.write(utf8(d));
                dataToSign.write(0);
            }
        }
    }

    public class TimeServlet extends HttpServlet {
        private long timeSlip = 0;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            long time = System.currentTimeMillis() + timeSlip;
            String timeStr = TimeUtils.formatISOUTCDateTime(new Date(time));
            resp.setContentLength(timeStr.length());
            resp.getWriter().write(timeStr);
        }
    }
}