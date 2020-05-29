/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.datapower;

import com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 4:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataPowerMonitorTest {
    public static final Logger logger = LoggerFactory.getLogger(DataPowerMonitorTest.class);
    private static final String REQUEST = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body xmlns:dp=\"http://www.datapower.com/schemas/management\"><dp:request><dp:get-status class=\"{0}\"/></dp:request></SOAP-ENV:Body></SOAP-ENV:Envelope>";
    private Map<String, Map<String, String>> expectedDataMap = new HashMap<String, Map<String, String>>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockDataPowerServer.startServerSSL();
    }

    @AfterClass
    public static void afterClass() {
        MockDataPowerServer.stopServer();
    }

    public DataPowerMonitorTest() {
        put("DP|test|Network|eth0|Outgoing Packets/sec", "1", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|eth1|Outgoing Packets/sec", "2", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|eth2|Outgoing Packets/sec", "3", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|eth3|Outgoing Packets/sec", "4", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|Total Outgoing Packets/sec", "10", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Used %", "21", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Total (MB)", numberToString(3368445D / 1024D), "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Used (MB)", numberToString(717004D / 1024D), "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Free (MB)", numberToString(2651441D / 1024D), "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Requested (MB)", numberToString(722628D / 1024D), "COLLECTIVE_OBSERVED_AVERAGE");

        //HTTPMeanTransactionTime
        put("DP|test|Transactions|helloworld_xmlfw|Average Response Time (ms)", "8", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Transactions|userws_proxy|Average Response Time (ms)", "17", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Transactions|Average Response Time (ms)", "12", "COLLECTIVE_OBSERVED_AVERAGE");
        //HTTPTransactions
        put("DP|test|Transactions|helloworld_xmlfw|Calls per Minute", "180", "COLLECTIVE_OBSERVED_CURRENT");
        put("DP|test|Transactions|userws_proxy|Calls per Minute", "420", "COLLECTIVE_OBSERVED_CURRENT");
        put("DP|test|Transactions|wsproxy|Calls per Minute", "0", "COLLECTIVE_OBSERVED_CURRENT");
        put("DP|test|Transactions|Calls per Minute", "600", "COLLECTIVE_OBSERVED_CURRENT");
    }

    @Test
    public void testServerUseBulkApi() {
        DataPowerMonitor monitor = new DataPowerMonitor();
        Map map = new HashMap();
        MetricFetcher task = monitor.createTask(map);
        Assert.assertTrue(task instanceof DataPowerMonitorTask);

        map.put("useBulkApi", true);
        task = monitor.createTask(map);
        Assert.assertTrue(task instanceof BulkApiMetricFetcher);

    }

    private String numberToString(Double val) {
        return new BigDecimal(val).setScale(0, RoundingMode.HALF_UP).toString();
    }

    private void put(String key1, String key2, String value) {
        Map<String, String> map = expectedDataMap.get(key1);
        if (map == null) {
            map = Maps.newHashMap();
            expectedDataMap.put(key1, map);
        }
        map.put(key2, value);
    }


}
