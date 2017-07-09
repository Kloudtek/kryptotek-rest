/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.client.httpcomponents;

import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import org.apache.http.auth.BasicUserPrincipal;
import org.apache.http.auth.Credentials;

import java.security.Principal;

/**
 * Created by yannick on 11/03/2015.
 */
public class RestAuthCredential implements Credentials {
    private String identity;
    private SigningKey clientKey;
    private SignatureVerificationKey serverKey;
    private DigestAlgorithm digestAlgorithm;
    private TimeSync timeSync;
    private Long timeDifferential;

    public RestAuthCredential(String identity, SigningKey clientKey, SignatureVerificationKey serverKey, DigestAlgorithm digestAlgorithm, TimeSync timeSync) {
        this.identity = identity;
        this.clientKey = clientKey;
        this.serverKey = serverKey;
        this.digestAlgorithm = digestAlgorithm;
        this.timeSync = timeSync;
    }

    public RestAuthCredential(String identity, SigningKey clientKey, SignatureVerificationKey serverKey, DigestAlgorithm digestAlgorithm) {
        this(identity, clientKey, serverKey, digestAlgorithm, null);
    }

    @Override
    public Principal getUserPrincipal() {
        return new BasicUserPrincipal(identity);
    }

    @Override
    public String getPassword() {
        return null;
    }

    public String getIdentity() {
        return identity;
    }

    public SigningKey getClientKey() {
        return clientKey;
    }

    public SignatureVerificationKey getServerKey() {
        return serverKey;
    }

    public DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public TimeSync getTimeSync() {
        return timeSync;
    }

    public Long getTimeDifferential() {
        return timeDifferential;
    }

    public void setTimeDifferential(Long timeDifferential) {
        this.timeDifferential = timeDifferential;
    }
}
