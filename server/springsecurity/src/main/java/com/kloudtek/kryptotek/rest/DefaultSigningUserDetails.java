package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.key.SignatureVerificationKey;
import com.kloudtek.kryptotek.key.SigningKey;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by yannick on 6/25/17.
 */
public class DefaultSigningUserDetails implements SigningUserDetails {
    private String username;
    private SignatureVerificationKey verificationKey;
    private SigningKey signingKey;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
    private Collection<? extends GrantedAuthority> grantedAuthorities;

    public DefaultSigningUserDetails(String username, SignatureVerificationKey verificationKey, SigningKey signingKey, GrantedAuthority... grantedAuthorities) {
        this(username, verificationKey, signingKey, true, true, true,
                true, grantedAuthorities);
    }

    public DefaultSigningUserDetails(String username, SignatureVerificationKey verificationKey, SigningKey signingKey,
                                     boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired,
                                     boolean enabled, GrantedAuthority... grantedAuthorities) {
        this.username = username;
        this.verificationKey = verificationKey;
        this.signingKey = signingKey;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.grantedAuthorities = Arrays.asList(grantedAuthorities);
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public SignatureVerificationKey getVerificationKey() {
        return verificationKey;
    }

    @Override
    public SigningKey getSigningKey() {
        return signingKey;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
