/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.client.httpcomponents;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by yannick on 27/10/2014.
 */
public interface TimeSync {
    long getTimeDifferential(HttpRequest request, HttpContext context) throws IOException;
}
