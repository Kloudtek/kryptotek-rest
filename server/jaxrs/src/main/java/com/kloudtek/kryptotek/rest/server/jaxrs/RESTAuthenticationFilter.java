/*
 * Copyright (c) 2016 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.server.jaxrs;

import com.kloudtek.kryptotek.CryptoEngine;
import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import com.kloudtek.kryptotek.rest.*;
import com.kloudtek.util.BackendAccessException;
import com.kloudtek.util.InvalidBackendDataException;
import com.kloudtek.util.TimeUtils;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.Principal;
import java.util.Date;
import java.util.List;

import static com.kloudtek.kryptotek.rest.RESTRequestSigner.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * Created by yannick on 28/10/2014.
 */
@SuppressWarnings("Duplicates")
public abstract class RESTAuthenticationFilter extends AuthenticationFilterHelper<Principal, ContainerRequestContext> implements ContainerRequestFilter, ContainerResponseFilter, WriterInterceptor {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RESTAuthenticationFilter.class);
    public static final String TMP_REQDETAILS = "X-TMP-REQDETAILS";

    public RESTAuthenticationFilter() {
    }

    public RESTAuthenticationFilter(CryptoEngine cryptoEngine) {
        super(cryptoEngine);
    }

    public RESTAuthenticationFilter(CryptoEngine cryptoEngine, ReplayAttackValidator replayAttackValidator) {
        super(cryptoEngine, replayAttackValidator);
    }

    public RESTAuthenticationFilter(CryptoEngine cryptoEngine, Long contentMaxSize, DigestAlgorithm digestAlgorithm, long expiry, ReplayAttackValidator replayAttackValidator) {
        super(cryptoEngine, contentMaxSize, digestAlgorithm, expiry, replayAttackValidator);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        URI reqUri = requestContext.getUriInfo().getRequestUri();
        try {
            Principal principal = authenticateRequest(requestContext.getEntityStream(), requestContext.getHeaderString(HEADER_NONCE),
                    requestContext.getHeaderString(HEADER_IDENTITY), requestContext.getHeaderString(HEADER_TIMESTAMP),
                    requestContext.getHeaderString(HEADER_SIGNATURE), requestContext.getMethod(), reqUri.getPath(),
                    reqUri.getRawQuery(), requestContext);
            RESTSecurityContext sc = new RESTSecurityContext(principal, requestContext.getSecurityContext().isSecure());
            requestContext.setSecurityContext(sc);
        } catch (AuthenticationFailedException e) {
            logger.warn(e.getMessage(), e);
            throw new WebApplicationException(e.getMessage(), e, UNAUTHORIZED);
        } catch (InvalidRequestException e) {
            logger.warn(e.getMessage(), e);
            throw new WebApplicationException(e.getMessage(), e, UNAUTHORIZED);
        } catch (InvalidBackendDataException e) {
            logger.error(e.getMessage(), e);
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void replaceDataStream(ContainerRequestContext requestContext, InputStream inputStream) {
        requestContext.setEntityStream(inputStream);
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext responseCtx) throws IOException, WebApplicationException {
        RequestDetails requestDetails = (RequestDetails) responseCtx.getProperty(TMP_REQDETAILS);
        if (requestDetails.principal != null) {
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            OutputStream oldStream = responseCtx.getOutputStream();
            responseCtx.setOutputStream(content);
            responseCtx.proceed();
            byte[] contentData = content.toByteArray();
            RESTResponseSigner responseSigner = new RESTResponseSigner(requestDetails.nonce, requestDetails.signature, requestDetails.statusCode, contentData);
            try {
                List<Object> signatures = responseCtx.getHeaders().get(HEADER_SIGNATURE);
                if (signatures != null && !signatures.isEmpty()) {
                    throw new IllegalStateException("Signature header already exists in response");
                }
                responseCtx.getHeaders().add(HEADER_TIMESTAMP, requestDetails.responseTimestamp);
                responseCtx.getHeaders().add(HEADER_SIGNATURE, signResponse(requestDetails.principal, responseSigner.getDataToSign()));
            } catch (InvalidBackendDataException e) {
                logger.error("Invalid key for identity " + requestDetails.identity + " : " + e.getMessage(), e);
                throw new WebApplicationException(INTERNAL_SERVER_ERROR);
            } catch (BackendAccessException e) {
                logger.error("Unexpected BackendAccessException" + e.getMessage(), e);
                throw new WebApplicationException(INTERNAL_SERVER_ERROR);
            }
            oldStream.write(contentData);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        RequestDetails requestDetails = new RequestDetails(requestContext.getHeaderString(HEADER_NONCE),
                requestContext.getHeaderString(HEADER_SIGNATURE), requestContext.getHeaderString(HEADER_IDENTITY),
                requestContext.getSecurityContext().getUserPrincipal(), responseContext.getStatus());
        if (responseContext.getEntity() == null && requestDetails.principal != null) {
            RESTResponseSigner responseSigner = new RESTResponseSigner(requestDetails.nonce, requestDetails.signature, requestDetails.statusCode, null);
            responseContext.getHeaders().add(HEADER_TIMESTAMP, requestDetails.responseTimestamp);
            try {
                responseContext.getHeaders().add(HEADER_SIGNATURE, signResponse(requestDetails.principal, responseSigner.getDataToSign()));
            } catch (InvalidBackendDataException e) {
                throw new WebApplicationException(INTERNAL_SERVER_ERROR);
            }
        } else {
            requestContext.setProperty(TMP_REQDETAILS, requestDetails);
        }
    }

    protected abstract Principal findUserPrincipal(String identity) throws BackendAccessException;

    protected abstract SignatureVerificationKey findVerificationKey(Principal principal) throws BackendAccessException;

    protected abstract SigningKey findSigningKey(Principal principal) throws BackendAccessException;

    public class RequestDetails {
        private String nonce;
        private String signature;
        private String identity;
        private String responseTimestamp;
        private Principal principal;
        private int statusCode;

        public RequestDetails(String nonce, String signature, String identity, Principal principal, int statusCode) {
            this.nonce = nonce;
            this.signature = signature;
            this.identity = identity;
            this.principal = principal;
            this.statusCode = statusCode;
            responseTimestamp = TimeUtils.formatISOUTCDateTime(new Date());
        }
    }
}
