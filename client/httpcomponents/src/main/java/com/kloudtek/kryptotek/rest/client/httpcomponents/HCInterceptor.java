/*
 * Copyright (c) 2016 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.client.httpcomponents;

import com.kloudtek.kryptotek.CryptoEngine;
import com.kloudtek.kryptotek.CryptoUtils;
import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import com.kloudtek.kryptotek.rest.RESTRequestSigner;
import com.kloudtek.kryptotek.rest.RESTResponseSigner;
import com.kloudtek.util.StringUtils;
import com.kloudtek.util.io.BoundedOutputStream;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.logging.Logger;

import static com.kloudtek.kryptotek.rest.RESTRequestSigner.*;
import static java.util.logging.Level.FINE;

/**
 * Created by yannick on 23/10/2014.
 */
public class HCInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {
    private static final Logger logger = Logger.getLogger(HCInterceptor.class.getName());
    public static final String KRYPTOTEK_REST_SIGNTOKEN = "kryptotek.rest.signtoken";
    public static final String AUTHORIZATION = "AUTHORIZATION";
    public static final String REQUEST_AUTHZ = "request_authz";
    private CryptoEngine cryptoEngine;
    private Long responseSizeLimit;

    public HCInterceptor(Long responseSizeLimit) {
        this(CryptoUtils.getEngine(), responseSizeLimit);
    }

    public HCInterceptor(@NotNull CryptoEngine cryptoEngine, Long responseSizeLimit) {
        this.cryptoEngine = cryptoEngine;
        this.responseSizeLimit = responseSizeLimit;
        logger.info("REST HCInterceptor using crypto engine: " + cryptoEngine.getClass().getName());
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        RestAuthCredential credentials = getCredentials(context);
        if (credentials != null) {
            Long timeDifferential = credentials.getTimeDifferential();
            TimeSync timeSync = credentials.getTimeSync();
            if (timeSync != null && timeDifferential == null) {
                // yes this is not synchronized, there's no harm in worse case scenario (worse can happen is syncing happening another time or two)
                timeDifferential = timeSync.getTimeDifferential(request, context);
                credentials.setTimeDifferential(timeDifferential);
            }
            RequestLine requestLine = request.getRequestLine();
            RESTRequestSigner requestSigner = new RESTRequestSigner(requestLine.getMethod(), requestLine.getUri(),
                    timeDifferential != null ? timeDifferential : 0, credentials.getIdentity());
            request.addHeader(HEADER_NONCE, requestSigner.getNonce());
            context.setAttribute(HEADER_NONCE, requestSigner.getNonce());
            request.addHeader(HEADER_TIMESTAMP, requestSigner.getTimestamp());
            request.addHeader(HEADER_IDENTITY, credentials.getIdentity());
            // TODO sign content-length and type
            byte[] content = getContent(request);
            if (content != null) {
                requestSigner.setContent(content);
            }
            try {
                String signature = sign(requestSigner.getDataToSign(), credentials.getClientKey(), credentials.getDigestAlgorithm());
                context.setAttribute(REQUEST_AUTHZ, signature);
                request.addHeader(HEADER_SIGNATURE, signature);
            } catch (Exception e) {
                throw new HttpException(e.getMessage(), e);
            }
        }
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (response.getStatusLine().getStatusCode() != 401) {
            Header[] signatures = response.getHeaders(HEADER_SIGNATURE);
            if (signatures == null || signatures.length != 1) {
                throw new HttpException("response is missing (or has more than one) " + HEADER_SIGNATURE + " header");
            }
            RestAuthCredential credentials = getCredentials(context);
            if (credentials != null) {
                RESTResponseSigner responseSigner = new RESTResponseSigner((String) context.getAttribute(HEADER_NONCE),
                        (String) context.getAttribute(REQUEST_AUTHZ), response.getStatusLine().getStatusCode());
                Header[] excludeBodyHeaders = response.getHeaders(HEADER_EXCLUDEBODY);
                responseSigner.setExcludeContent(excludeBodyHeaders.length > 0 && Boolean.parseBoolean(excludeBodyHeaders[0].getValue()));
                HttpEntity entity = loadEntity(response, responseSizeLimit);
                byte[] content = getContent(entity);
                if (content != null) {
                    responseSigner.setContent(content);
                }
                try {
                    verifySignature(signatures[0].getValue(), responseSigner.getDataToSign(), credentials.getServerKey(),
                            credentials.getDigestAlgorithm());
                } catch (InvalidKeyException e) {
                    throw new HttpException(e.getMessage(), e);
                } catch (SignatureException e) {
                    throw new HttpException("Invalid response signature");
                }
            }
        }
    }

    private RestAuthCredential getCredentials(HttpContext context) {
        HttpHost targetHost = ((HttpClientContext) context).getTargetHost();
        int port = targetHost.getPort();
        if (port == -1) {
            port = targetHost.getSchemeName().equals("https") ? 443 : 80;
        }
        Credentials credentials = ((HttpClientContext) context).getCredentialsProvider().getCredentials(new AuthScope(targetHost.getHostName(), port));
        if (credentials instanceof RestAuthCredential) {
            return (RestAuthCredential) credentials;
        } else {
            return null;
        }
    }

    private String sign(byte[] data, SigningKey clientKey, DigestAlgorithm digestAlgorithm) throws InvalidKeyException, SignatureException {
        if (logger.isLoggable(FINE)) {
            logger.fine("Signing REST request - key: " + CryptoUtils.fingerprint(clientKey.getEncoded().getEncodedKey()) + " alg: " + digestAlgorithm + " data: " + CryptoUtils.fingerprint(data));
        }
        return StringUtils.base64Encode(cryptoEngine.sign(clientKey, digestAlgorithm, data));
    }

    private void verifySignature(String signature, byte[] signedData, SignatureVerificationKey serverKey, DigestAlgorithm digestAlgorithm) throws InvalidKeyException, SignatureException {
        final byte[] signatureData = StringUtils.base64Decode(signature);
        if (logger.isLoggable(FINE)) {
            logger.fine("Verifying REST response - key: " + CryptoUtils.fingerprint(serverKey.getEncoded().getEncodedKey())
                    + " alg: " + digestAlgorithm + " data: " + CryptoUtils.fingerprint(signedData) + " signature: " + CryptoUtils.fingerprint(signatureData));
        }
        cryptoEngine.verifySignature(serverKey, digestAlgorithm, signedData, signatureData);
    }

    public HttpClientBuilder add(HttpClientBuilder builder) {
        return builder.addInterceptorLast((HttpRequestInterceptor) this).addInterceptorFirst((HttpResponseInterceptor) this);
    }

    public HttpClientBuilder createClientBuilder() {
        return add(HttpClientBuilder.create());
    }

    public CloseableHttpClient createClient(CredentialsProvider credentialsProvider) {
        return createClientBuilder().setDefaultCredentialsProvider(credentialsProvider).build();
    }

    public Long getResponseSizeLimit() {
        return responseSizeLimit;
    }

    public void setResponseSizeLimit(Long responseSizeLimit) {
        this.responseSizeLimit = responseSizeLimit;
    }

    private static byte[] getContent(HttpRequest request) throws IOException {
        return getContent(loadEntity(request));
    }

    private static byte[] getContent(HttpEntity entity) throws IOException {
        if (entity != null) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            entity.writeTo(buf);
            buf.close();
            return buf.toByteArray();
        } else {
            return null;
        }
    }

    private static HttpEntity loadEntity(HttpRequest request) throws IOException {
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity originalEntity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (originalEntity != null) {
                HttpEntity loadedEntity = loadEntity(originalEntity, null);
                ((HttpEntityEnclosingRequest) request).setEntity(loadedEntity);
                return loadedEntity;
            }
        }
        return null;
    }

    private HttpEntity loadEntity(HttpResponse response, Long responseSizeLimit) throws IOException {
        if (response.getEntity() != null) {
            HttpEntity entity = loadEntity(response.getEntity(), responseSizeLimit);
            response.setEntity(entity);
            return entity;
        } else {
            return null;
        }
    }

    private static HttpEntity loadEntity(HttpEntity entity, Long limit) throws IOException {
        if (entity.isRepeatable()) {
            return entity;
        } else {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (limit != null) {
                entity.writeTo(new BoundedOutputStream(buffer, limit, true));
            } else {
                entity.writeTo(buffer);
            }
            buffer.close();
            return new ByteArrayEntity(buffer.toByteArray());
        }
    }
}
