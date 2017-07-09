/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.client.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * Created by yannick on 29/08/15.
 */
@Path("test")
public interface RESTInterface {
    @Path("afdsfdsafsda")
    @GET
    String doStuff(@QueryParam("a") String a, byte[] data);
}
