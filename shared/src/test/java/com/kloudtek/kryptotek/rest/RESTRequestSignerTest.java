/*
 * Copyright (c) 2016 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.CryptoUtils;
import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.key.HMACKey;
import com.kloudtek.util.StringUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by yannick on 17/2/16.
 */
public class RESTRequestSignerTest {
    public static final String NONCE = "e44fc2ca-460a-4e82-b88f-41dca6cdec19";

    @Test
    public void testSigning() throws Exception {
        HMACKey hmacKey = CryptoUtils.readHMACKey(DigestAlgorithm.SHA256, StringUtils.base64Decode("9vY7UUGRlpuk4MxXdTPPbqqFwcKVvpMbFwbGv+b/V2c="));
        RESTRequestSigner requestSigner = new RESTRequestSigner("GET", "https://api.idvkey.com/someapi", NONCE, "2016-03-30T23:54:46", "somekeyid");
        requestSigner.setContent("hello world".getBytes());
        byte[] dataToSign = requestSigner.getDataToSign();
        byte[] sig = CryptoUtils.sign(hmacKey, dataToSign);
        assertEquals(sig, StringUtils.base64Decode("aFRyoKNJxKPtZW4+cywAdiYcIbUbb9ZH5wkzxkG0t0Y="));
        RESTResponseSigner responseSigner = new RESTResponseSigner(NONCE, StringUtils.base64Encode(sig), 200);
        responseSigner.setContent(StringUtils.utf8("success"));
        byte[] rsig = CryptoUtils.sign(hmacKey, responseSigner.getDataToSign());
        assertEquals(rsig, StringUtils.base64Decode("f2yV/Zi8U/Boi9Ui0A5r3/FIYhyG5xv5PrfW9hDIMLA="));
    }
}