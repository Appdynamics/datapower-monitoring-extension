/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.yml.YmlReader;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.appdynamics.monitors.util.Xml;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by abey.tom on 7/31/15.
 */
public class BulkApiMetricFetcherTest {
    public static final Logger logger = LoggerFactory.getLogger(BulkApiMetricFetcherTest.class);

    @Test
    public void run() {
        final Map<String, String> expectedMap = getExpectedMap();
        DataPowerMonitor monitor = Mockito.spy(new DataPowerMonitor());
        final SoapMessageUtil soapMessageUtil = new SoapMessageUtil();
        //Read the YAML and metrics.xml
        Map<String, ?> config = YmlReader.readFromFileAsMap(new File(getClass().getResource("/conf/config.yml").getFile()));

        Stat[] metricConfig = null;
//                monitor.readStatsInfoFile(getClass().getResourceAsStream("/conf/metrics.xml"));
        Map server = new HashMap();
        server.put("uri", "http://localhost:5550/service/mgmt/current");
        server.put("domains", Arrays.asList("domain1","domain2"));

        AMonitorJob monitorJob = Mockito.mock(AMonitorJob.class);
        MonitorContextConfiguration c = new MonitorContextConfiguration("DataPowerMonitor","Custom Metrics|X|",new File("Dummy"), monitorJob);
        c.setConfigYml("src/main/resources/conf/config.yml");

        //c.setMetricsXml("/metrics/test-metrics.xml",Stat.Stats.class);
        //Create the Task with builder
        BulkApiMetricFetcher original = (BulkApiMetricFetcher) new MetricFetcher.Builder(true)
                .server(server)
                .configuration(c)
                .soapMessageUtil(soapMessageUtil)
                .metricWriter(monitor)
                .build();
        BulkApiMetricFetcher fetcher = Mockito.spy(original);

        //Mock the response
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                InputStream in = getClass().getResourceAsStream("/output/BulkResponse.xml");
                String str = IOUtils.toString(in);
                Map<String, Xml[]> xmlMap = soapMessageUtil.getSoapResponseBody(str,
                        Arrays.asList("CPUUsage", "MemoryStatus", "ReceiveKbpsThroughput"));
                return xmlMap;
            }
        }).when(fetcher).getResponse(Mockito.anyCollection(), Mockito.anyString());

        //Mock the print metric and compare the output
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object key = invocation.getArguments()[0];
                Object value = invocation.getArguments()[1];
                String remove = expectedMap.remove(key);
                logger.info("Checking {} for {} vs {}",key,value,remove);
//                Assert.assertEquals(value, remove);
                return null;
            }
        }).when(monitor).printMetric(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        //execute
        fetcher.run();

        //Validate
        logger.info("The map contents are {}", expectedMap);
//        Assert.assertTrue(expectedMap.isEmpty());
    }

    private Map<String, String> getExpectedMap() {
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("Custom Metrics|X|System|Memory|Used %", "20");
        expected.put("Custom Metrics|X|System|Memory|Total (MB)", "17920");
        expected.put("Custom Metrics|X|System|Memory|Used (MB)", "3721");
        expected.put("Custom Metrics|X|System|Memory|Free (MB)", "14199");
        expected.put("Custom Metrics|X|System|Memory|Requested (MB)", "14572");
        expected.put("Custom Metrics|X|System|CPU Usage %", "10");
        expected.put("Custom Metrics|X|default|Network|(illegal)|Incoming KB/sec", "996");
        expected.put("Custom Metrics|X|default|Network|mgt0|Incoming KB/sec", "78");
        expected.put("Custom Metrics|X|default|Network|eth10|Incoming KB/sec", "0");
        expected.put("Custom Metrics|X|default|Network|mgt1|Incoming KB/sec", "0");
        expected.put("Custom Metrics|X|default|Network|eth11|Incoming KB/sec", "0");
        expected.put("Custom Metrics|X|default|Network|eth12|Incoming KB/sec", "0");
        expected.put("Custom Metrics|X|default|Network|eth13|Incoming KB/sec", "0");
        expected.put("Custom Metrics|X|default|Network|eth20|Incoming KB/sec", "294");
        expected.put("Custom Metrics|X|default|Network|eth21|Incoming KB/sec", "53");
        expected.put("Custom Metrics|X|default|Network|Total Incoming KB/sec", "1421");
        return expected;
    }
}