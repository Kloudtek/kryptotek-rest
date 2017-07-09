package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Created by yannick on 6/25/17.
 */
public interface SigningUserDetails extends UserDetails {
    SignatureVerificationKey getVerificationKey();
    SigningKey getSigningKey();
}
