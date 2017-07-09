/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.server.jaxrs;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Created by yannick on 06/04/15.
 */
public class RESTSecurityContext implements SecurityContext {
    private Principal principal;
    private boolean secure;

    public RESTSecurityContext(Principal principal, boolean secure) {
        this.principal = principal;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal instanceof RolePrincipal && ((RolePrincipal) principal).isInRole(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "SIGNED";
    }
}
