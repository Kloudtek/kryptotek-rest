package com.kloudtek.kryptotek.rest;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Created by yannick on 6/25/17.
 */
public class SignedRequestAuthenticationToken extends AbstractAuthenticationToken {
    private UserDetails userDetails;

    public SignedRequestAuthenticationToken(UserDetails userDetails) {
        super(userDetails.getAuthorities());
        this.userDetails = userDetails;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userDetails;
    }
}
