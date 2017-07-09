/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.server.jaxrs;

import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yannick on 29/10/2014.
 */
@SuppressWarnings("unchecked")
public class TestApp extends Application {
    HashSet<Class<?>> classes = new HashSet<Class<?>>(Arrays.asList(TestService.class,Verifier.class));

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
