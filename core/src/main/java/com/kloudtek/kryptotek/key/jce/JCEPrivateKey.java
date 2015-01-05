/*
 * Copyright (c) 2014 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.key.jce;

import com.kloudtek.kryptotek.CryptoAlgorithm;
import com.kloudtek.kryptotek.EncodedKey;
import com.kloudtek.kryptotek.InvalidKeyEncodingException;
import com.kloudtek.kryptotek.JCECryptoEngine;
import com.kloudtek.kryptotek.key.PrivateKey;

import java.security.InvalidKeyException;

/**
 * Created by yannick on 20/12/2014.
 */
public abstract class JCEPrivateKey extends AbstractJCEKey<java.security.PrivateKey> implements JCEKey, PrivateKey {
    public JCEPrivateKey(JCECryptoEngine cryptoEngine, java.security.PrivateKey privateKey) {
        super(cryptoEngine, privateKey);
    }

    public JCEPrivateKey(JCECryptoEngine cryptoEngine, EncodedKey encodedKey) throws InvalidKeyException, InvalidKeyEncodingException {
        super(cryptoEngine, encodedKey);
    }

    public java.security.PrivateKey getJCEPrivateKey() {
        return key;
    }
}
