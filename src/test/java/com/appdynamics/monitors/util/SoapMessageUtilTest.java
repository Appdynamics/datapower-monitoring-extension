/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.util;

import com.appdynamics.extensions.xml.Xml;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by abey.tom on 7/31/15.
 */
public class SoapMessageUtilTest {
    public static final Logger logger = LoggerFactory.getLogger(SoapMessageUtilTest.class);

    @Test
    public void testGetSoapResponseBody() throws Exception {
        SoapMessageUtil soapMessageUtil = new SoapMessageUtil();
        InputStream in = getClass().getResourceAsStream("/output/BulkResponse.xml");
        String str = IOUtils.toString(in);
        Map<String, Xml[]> xmlMap = soapMessageUtil.getSoapResponseBody(str, Arrays.asList("CPUUsage", "MemoryStatus", "ReceiveKbpsThroughput"));
        for (String s : xmlMap.keySet()) {
            Xml[] xmls = xmlMap.get(s);
            logger.info("The operation is [{}] and response is {}", s, Arrays.toString(xmls));
        }
    }
}