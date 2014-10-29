/*
 * Copyright (c) 2014 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.DigestUtils;
import com.kloudtek.util.StringUtils;
import com.kloudtek.util.TimeUtils;
import com.kloudtek.util.validation.ValidationUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static com.kloudtek.kryptotek.CryptoUtils.fingerprint;
import static com.kloudtek.util.StringUtils.utf8;

/**
 * Created by yannick on 28/10/2014.
 */
public class RESTRequestSigner {
    public static final String HEADER_NOUNCE = "X-NOUNCE";
    public static final String HEADER_TIMESTAMP = "X-TIMESTAMP";
    public static final String HEADER_IDENTITY = "X-IDENTITY";
    public static final String HEADER_SIGNATURE = "X-SIGNATURE";
    private String method;
    private String uri;
    private String nounce;
    private String timestamp;
    private String identity;
    private byte[] content;

    public RESTRequestSigner(String method, String uri, String nounce, String timestamp, String identity) {
        this.method = method;
        this.uri = uri;
        this.nounce = nounce;
        this.timestamp = timestamp;
        this.identity = identity;
    }

    public RESTRequestSigner(String method, String uri, long timeDifferential, String identity) {
        this(method,uri,timeDifferential,identity,null);
    }

    public RESTRequestSigner(String method, String uri, long timeDifferential, String identity, byte[] content) {
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

    public String getNounce() {
        return nounce;
    }

    public void setNounce(String nounce) {
        this.nounce = nounce;
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
        if (nounce != null ? !nounce.equals(that.nounce) : that.nounce != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (nounce != null ? nounce.hashCode() : 0);
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
                ", nounce='" + nounce + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", identity='" + identity + '\'' +
                ", content=" +(content != null ? fingerprint(content) : "null") +
                '}';
    }

    public byte[] getDataToSign() throws IOException {
        if( ! ValidationUtils.notEmpty(method,uri,nounce,timestamp,identity)) {
            throw new IllegalArgumentException("Not all signing parameters have been set");
        }
        String dataToSign = new StringBuilder(method.toUpperCase().trim()).append('\n').append(uri.trim()).append('\n')
                .append(nounce).append('\n').append(timestamp.trim().toUpperCase()).append('\n').append(identity).toString();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(StringUtils.utf8(dataToSign));
        if( content != null ) {
            buf.write(content);
        }
        buf.close();
        return buf.toByteArray();
    }
}
