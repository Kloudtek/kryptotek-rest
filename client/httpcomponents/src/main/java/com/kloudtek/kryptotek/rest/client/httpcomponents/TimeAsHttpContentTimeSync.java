/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest.client.httpcomponents;

import com.kloudtek.util.TimeUtils;
import com.kloudtek.util.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by yannick on 27/10/2014.
 */
public class TimeAsHttpContentTimeSync implements TimeSync {
    private String url;

    public TimeAsHttpContentTimeSync(String url) {
        this.url = url;
    }

    @Override
    public long getTimeDifferential(HttpRequest request, HttpContext context) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Unable to sync time, server returned error code " + response.getStatusLine().getStatusCode() + " : " + response.getStatusLine().getReasonPhrase());
            }
            long now = System.currentTimeMillis();
            String serverTimestamp = IOUtils.toString(response.getEntity().getContent());
            try {
                return now - TimeUtils.parseISOUTCDateTime(serverTimestamp).getTime();
            } catch (ParseException e) {
                throw new IOException("Unable to sync time, invalid time returned by server: " + serverTimestamp);
            }
        } finally {
            IOUtils.close(httpClient);
        }
    }
}
