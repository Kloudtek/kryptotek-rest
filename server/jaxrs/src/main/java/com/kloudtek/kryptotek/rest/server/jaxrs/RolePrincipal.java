/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.server.jaxrs;

import java.security.Principal;

/**
 * Created by yannick on 06/04/15.
 */
public interface RolePrincipal extends Principal {
    boolean isInRole(String role);
}
