package com.kloudtek.kryptotek.rest.server;

import com.kloudtek.kryptotek.CryptoUtils;
import com.kloudtek.kryptotek.jce.JCECryptoEngine;
import com.kloudtek.kryptotek.jce.JCEHMACSHA1Key;
import com.kloudtek.kryptotek.key.HMACKey;
import com.kloudtek.kryptotek.rest.RESTRequestSigner;
import com.kloudtek.kryptotek.rest.RESTResponseSigner;
import com.kloudtek.util.StringUtils;
import com.kloudtek.util.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.logging.Logger;

import static com.kloudtek.kryptotek.rest.RESTRequestSigner.*;

/**
 * Created by yannick on 6/24/17.
 */
public class TestHelper {
    private static final Logger logger = Logger.getLogger(TestHelper.class.getName());
    private CloseableHttpClient httpClient;
    public static final String HMAC_KEY_B64 = "cni1ZN5Q3HKv8KAbPy878xWnJzwE/3MyG9vU3M5MAOHiLJXJVeYCnNQVN6e7H/T7mo7EJn3ATLOIjtGJwPkOvA==";
    public static final HMACKey HMAC_KEY = new JCEHMACSHA1Key(new JCECryptoEngine(), new SecretKeySpec(StringUtils.base64Decode(HMAC_KEY_B64), "RAW"));
    public static final String DATA_STR = "blabla";
    public static final byte[] DATA = DATA_STR.getBytes();
    public static final String PATH = "/test/dostuff?x=" + StringUtils.urlEncode("a b");
    public static final String USER = "user";
    private String url;

    public TestHelper(String url) {
        this.url = url;
    }

    public void testValidHmac() throws IOException, InvalidKeyException {
        httpClient = HttpClientBuilder.create().build();
        try {
            RESTRequestSigner restRequestSigner = new RESTRequestSigner("POST", PATH, 0, USER, DATA);
            HttpPost request = new HttpPost(url + PATH);
            request.setHeader(HEADER_IDENTITY, restRequestSigner.getIdentity());
            request.setHeader(HEADER_NONCE, restRequestSigner.getNonce());
            request.setHeader(HEADER_TIMESTAMP, restRequestSigner.getTimestamp());
            String signature = StringUtils.base64Encode(CryptoUtils.sign(HMAC_KEY, restRequestSigner.getDataToSign()));
            request.setHeader(HEADER_SIGNATURE, signature);
            request.setHeader("Accept", "application/json");
            request.setEntity(new ByteArrayEntity(DATA));
            logger.info(restRequestSigner.toString());
            CloseableHttpResponse response = httpClient.execute(request);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
            byte[] responseData = IOUtils.toByteArray(response.getEntity().getContent());
            Assert.assertEquals(new String(responseData), "{\"a\":\"b\",\"b\":\"c\"}");
            String expectedSig = StringUtils.base64Encode(CryptoUtils.sign(HMAC_KEY, new RESTResponseSigner(restRequestSigner.getNonce(), signature, 200, responseData).getDataToSign()));
            Assert.assertEquals(response.getFirstHeader(HEADER_SIGNATURE).getValue(),expectedSig);
        } finally {
            httpClient.close();
        }
    }

    public void testExpiredHmac() throws IOException, InvalidKeyException {
        httpClient = HttpClientBuilder.create().build();
        try {
            RESTRequestSigner restRequestSigner = new RESTRequestSigner("POST", PATH, -1000000L, USER, DATA);
            HttpPost request = new HttpPost(url + PATH);
            request.setHeader(HEADER_IDENTITY, restRequestSigner.getIdentity());
            request.setHeader(HEADER_NONCE, restRequestSigner.getNonce());
            request.setHeader(HEADER_TIMESTAMP, restRequestSigner.getTimestamp());
            String signature = StringUtils.base64Encode(CryptoUtils.sign(HMAC_KEY, restRequestSigner.getDataToSign()));
            request.setHeader(HEADER_SIGNATURE, signature);
            request.setEntity(new ByteArrayEntity(DATA));
            logger.info(restRequestSigner.toString());
            CloseableHttpResponse response = httpClient.execute(request);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 401);
        } finally {
            httpClient.close();
        }
    }

    public void testInvalidHmac() throws Exception {
        httpClient = HttpClientBuilder.create().build();
        try {
            RESTRequestSigner restRequestSigner = new RESTRequestSigner("POST", PATH, 0, USER, "asfdasfd".getBytes());
            HttpPost request = new HttpPost(url + PATH);
            request.setHeader(HEADER_IDENTITY, restRequestSigner.getIdentity());
            request.setHeader(HEADER_NONCE, restRequestSigner.getNonce());
            request.setHeader(HEADER_TIMESTAMP, restRequestSigner.getTimestamp());
            String signature = StringUtils.base64Encode(CryptoUtils.sign(HMAC_KEY, restRequestSigner.getDataToSign()));
            request.setHeader(HEADER_SIGNATURE, signature);
            request.setEntity(new ByteArrayEntity(DATA));
            logger.info(restRequestSigner.toString());
            CloseableHttpResponse response = httpClient.execute(request);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 401);
        } finally {
            httpClient.close();
        }
    }

    public void testException(boolean signedRespBody) throws Exception {
        httpClient = HttpClientBuilder.create().build();
        try {
            RESTRequestSigner restRequestSigner = new RESTRequestSigner("POST", "/test/exception1", 0, USER, DATA);
            HttpPost request = new HttpPost(url + "/test/exception1");
            request.setHeader(HEADER_IDENTITY, restRequestSigner.getIdentity());
            request.setHeader(HEADER_NONCE, restRequestSigner.getNonce());
            request.setHeader(HEADER_TIMESTAMP, restRequestSigner.getTimestamp());
            String signature = StringUtils.base64Encode(CryptoUtils.sign(HMAC_KEY, restRequestSigner.getDataToSign()));
            request.setHeader(HEADER_SIGNATURE, signature);
            request.setEntity(new ByteArrayEntity(DATA));
            logger.info(restRequestSigner.toString());
            CloseableHttpResponse response = httpClient.execute(request);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 400);
            byte[] responseData = IOUtils.toByteArray(response.getEntity().getContent());
            if( ! signedRespBody ) {
                Assert.assertEquals(response.getFirstHeader(HEADER_EXCLUDEBODY).getValue(), "true");
            }
            String expectedSig = StringUtils.base64Encode(CryptoUtils.sign(HMAC_KEY, new RESTResponseSigner(restRequestSigner.getNonce(), signature, 400, !signedRespBody, responseData).getDataToSign()));
            Assert.assertEquals(response.getFirstHeader(HEADER_SIGNATURE).getValue(), expectedSig);
        } finally {
            httpClient.close();
        }
    }
}
