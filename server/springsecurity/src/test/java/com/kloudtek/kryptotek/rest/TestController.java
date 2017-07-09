package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.rest.server.TestHelper;
import com.kloudtek.util.io.IOUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.testng.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Created by yannick on 6/24/17.
 */
@RestController
@RequestMapping("/test")
public class TestController {
    private static final Logger logger = Logger.getLogger(TestController.class.getName());

    @RequestMapping(path = "/dostuff",method = POST,produces = "application/json")
    public Map<String, String> doStuff(@RequestParam("x") String x, InputStream content) throws IOException {
        Assert.assertEquals(x, "a b");
        String contentData = IOUtils.toString(content);
        Assert.assertEquals(contentData, TestHelper.DATA_STR);
        LinkedHashMap<String, String> results = new LinkedHashMap<String, String>();
        results.put("a", "b");
        results.put("b", "c");
        return results;
    }

    @RequestMapping(path = "/exception1",method = POST,produces = "application/json")
    public String doStuff() throws IOException {
        throw new HttpMessageNotReadableException("moo");
    }
}
