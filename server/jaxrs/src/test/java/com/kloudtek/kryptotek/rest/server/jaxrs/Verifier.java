/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.server.jaxrs;

import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import com.kloudtek.kryptotek.rest.server.TestHelper;
import com.kloudtek.util.BackendAccessException;

import java.security.Principal;
import java.util.logging.Logger;

/**
 * Created by yannick on 29/10/2014.
 */
public class Verifier extends RESTAuthenticationFilter {
    private static final Logger logger = Logger.getLogger(Verifier.class.getName());
    private DigestAlgorithm digestAlgorithm;

    public Verifier() {
        this.digestAlgorithm = DigestAlgorithm.SHA1;
    }

    @Override
    protected Principal findUserPrincipal(final String identity) throws BackendAccessException {
        return new Principal() {
            @Override
            public String getName() {
                return identity;
            }
        };
    }

    protected SigningKey findSigningKey(Principal principal) {
        if (principal.getName().equals(TestHelper.USER)) {
            return TestHelper.HMAC_KEY;
        } else {
            return null;
        }
    }

    protected SignatureVerificationKey findVerificationKey(Principal principal) {
        if (principal.getName().equals(TestHelper.USER)) {
            return TestHelper.HMAC_KEY;
        } else {
            return null;
        }
    }
}
