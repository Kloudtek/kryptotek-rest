/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.server.jaxrs;

import com.kloudtek.kryptotek.rest.server.TestHelper;
import com.kloudtek.util.io.IOUtils;
import org.testng.Assert;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by yannick on 29/10/2014.
 */
@Path("/test")
public class TestService {
    private static final Logger logger = Logger.getLogger(TestService.class.getName());

    @Path("/dostuff")
    @POST
    @Produces("application/json")
    public Map<String,String> doStuff( @QueryParam("x") String x, InputStream content) throws IOException {
        Assert.assertEquals(x,"a b");
        String contentData = IOUtils.toString(content);
        Assert.assertEquals(contentData, TestHelper.DATA_STR);
        LinkedHashMap<String,String> results = new LinkedHashMap<String, String>();
        results.put("a","b");
        results.put("b","c");
        return results;
    }

    @Path("/exception1")
    @POST
    public String doStuff() throws IOException {
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
}
