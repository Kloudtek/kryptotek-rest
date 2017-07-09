package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.CryptoEngine;
import com.kloudtek.kryptotek.DigestAlgorithm;
import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import com.kloudtek.util.BackendAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.servlet.ServletRequest;
import java.io.InputStream;
import java.security.Principal;

/**
 * Spring Security authentication filter helper
 */
public class SpringAuthenticationFilterHelper extends AuthenticationFilterHelper<SigningUserDetails, ServletRequest> {
    public static final String STREAM_ATTR = "ktreplacementstream";
    private UserDetailsService userDetailsService;

    public SpringAuthenticationFilterHelper(@Autowired UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public SpringAuthenticationFilterHelper(@Autowired UserDetailsService userDetailsService, CryptoEngine cryptoEngine) {
        super(cryptoEngine);
        this.userDetailsService = userDetailsService;
    }

    public SpringAuthenticationFilterHelper(@Autowired UserDetailsService userDetailsService, CryptoEngine cryptoEngine, ReplayAttackValidator replayAttackValidator) {
        super(cryptoEngine, replayAttackValidator);
        this.userDetailsService = userDetailsService;
    }

    public SpringAuthenticationFilterHelper(@Autowired UserDetailsService userDetailsService, CryptoEngine cryptoEngine, Long contentMaxSize, DigestAlgorithm digestAlgorithm, long expiry, ReplayAttackValidator replayAttackValidator) {
        super(cryptoEngine, contentMaxSize, digestAlgorithm, expiry, replayAttackValidator);
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void replaceDataStream(ServletRequest requestObj, InputStream inputStream) {
        requestObj.setAttribute(STREAM_ATTR,inputStream);
    }

    @Override
    protected SigningUserDetails findUserPrincipal(String identity) throws BackendAccessException {
        try {
            return (SigningUserDetails) userDetailsService.loadUserByUsername(identity);
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

    @Override
    public SignatureVerificationKey findVerificationKey(SigningUserDetails signingUserDetails) throws BackendAccessException {
        return signingUserDetails.getVerificationKey();
    }

    @Override
    public SigningKey findSigningKey(SigningUserDetails signingUserDetails) throws BackendAccessException {
        return signingUserDetails.getSigningKey();
    }
}
