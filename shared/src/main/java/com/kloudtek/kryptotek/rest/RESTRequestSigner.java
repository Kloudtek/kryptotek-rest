/*
 * Copyright (c) 2016 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.CryptoUtils;
import com.kloudtek.util.StringUtils;
import com.kloudtek.util.TimeUtils;
import com.kloudtek.util.validation.ValidationUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Created by yannick on 28/10/2014.
 */
public class RESTRequestSigner {
    public static final String HEADER_NONCE = "X-NONCE";
    public static final String HEADER_TIMESTAMP = "X-TIMESTAMP";
    public static final String HEADER_IDENTITY = "X-IDENTITY";
    public static final String HEADER_SIGNATURE = "X-SIGNATURE";
    public static final String HEADER_EXCLUDEBODY = "X-SIGNATURE-EXCLUDEBODY";
    private String method;
    private String uri;
    private String nonce;
    private String timestamp;
    private String identity;
    private byte[] content;

    public RESTRequestSigner(@NotNull String method, @NotNull String uri, @NotNull String nonce, @NotNull String timestamp, @NotNull String identity) {
        this.method = method.toUpperCase();
        this.uri = uri;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.identity = identity;
    }

    public RESTRequestSigner(@NotNull String method, @NotNull String uri, long timeDifferential, @NotNull String identity) {
        this(method,uri,timeDifferential,identity,null);
    }

    public RESTRequestSigner(@NotNull String method, @NotNull String uri, long timeDifferential, @NotNull String identity, byte[] content) {
        this(method, uri, UUID.randomUUID().toString(), TimeUtils.formatISOUTCDateTime(new Date(System.currentTimeMillis() - timeDifferential)), identity );
        this.content = content;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RESTRequestSigner that = (RESTRequestSigner) o;

        if (!Arrays.equals(content, that.content)) return false;
        if (identity != null ? !identity.equals(that.identity) : that.identity != null) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (nonce != null ? !nonce.equals(that.nonce) : that.nonce != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        return !(uri != null ? !uri.equals(that.uri) : that.uri != null);

    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (nonce != null ? nonce.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (identity != null ? identity.hashCode() : 0);
        result = 31 * result + (content != null ? Arrays.hashCode(content) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RESTRequestSigner{" +
                "method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", nonce='" + nonce + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", identity='" + identity + '\'' +
                ", content=" +(content != null ? CryptoUtils.fingerprint(content) : "null") +
                '}';
    }

    public byte[] getDataToSign() throws IOException {
        if (!ValidationUtils.notEmpty(method, uri, nonce, timestamp, identity)) {
            throw new IllegalArgumentException("Not all signing parameters have been set");
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(StringUtils.utf8(method.toUpperCase().trim() + '\n' + uri.trim() + '\n' + nonce + '\n' + timestamp.trim().toUpperCase() + '\n' + identity + '\n'));
        if( content != null ) {
            buf.write(content);
        }
        return buf.toByteArray();
    }
}
