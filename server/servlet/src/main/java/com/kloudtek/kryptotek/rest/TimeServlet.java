/*
 * Copyright (c) 2015 Kloudtek Ltd
 */

package com.kloudtek.kryptotek.rest;

import com.kloudtek.util.TimeUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Created by yannick on 28/10/2014.
 */
public class TimeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long time = System.currentTimeMillis();
        String timeStr = TimeUtils.formatISOUTCDateTime(new Date(time));
        resp.setContentLength(timeStr.length());
        resp.getWriter().write(timeStr);
    }
}
