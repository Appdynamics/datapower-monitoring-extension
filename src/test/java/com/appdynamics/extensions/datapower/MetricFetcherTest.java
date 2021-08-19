/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.datapower.config.Stat;
import com.appdynamics.extensions.datapower.util.SoapMessageUtil;
import com.appdynamics.extensions.util.TestHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by abey.tom on 7/31/15.
 */
public class MetricFetcherTest {
    @Test
    public void getDomainsRegexTest() {
        MetricFetcher monitor = Mockito.spy(new DataPowerMonitorTask());
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return TestHelper.getResponse("DomainStatus");
            }
        }).when(monitor).getResponse(Mockito.anyString(), Mockito.anyString());


        //Run 0
        List<String> domains = monitor.getMatchingDomains(Arrays.asList("Domain.*", "Not.*"));
        Assert.assertTrue(domains.size() == 3);
        Assert.assertTrue(domains.contains("Domain1"));
        Assert.assertTrue(domains.contains("Domain2"));
        Assert.assertTrue(domains.contains("NotD0main"));

        //Run 1
        domains = monitor.getMatchingDomains(Arrays.asList("Not.*"));
        Assert.assertTrue(domains.size() == 1);
        Assert.assertTrue(domains.contains("NotD0main"));

        //Run 2 -
        domains = monitor.getMatchingDomains(Arrays.asList("Domain2"));
        Assert.assertTrue(domains.size() == 1);
        Assert.assertTrue(domains.contains("Domain2"));

        //Run 3 - If nothing matches add defalt
        domains = monitor.getMatchingDomains(Arrays.asList("Domain4"));
        Assert.assertTrue(domains.size() == 0);

        //If not specified add nothing
        domains = monitor.getMatchingDomains(Arrays.asList(".*"));
        Assert.assertTrue(domains.size() == 3);

    }

    @Test
    public void testFetchMetric() {
        final SoapMessageUtil soapMessageUtil = new SoapMessageUtil();
        //Read the YAML and metrics.xml
        AMonitorJob monitorJob = Mockito.mock(AMonitorJob.class);
        MonitorContextConfiguration c = new MonitorContextConfiguration("DataPowerMonitor","Custom Metrics|X|", new File("Dummy"), monitorJob);
        c.setConfigYml("src/main/resources/conf/config.yml");
        c.setMetricXml("src/test/resources/metrics/test-metrics.xml",Stat.Stats.class);
        //Create the Task with builder
        DataPowerMonitorTask original = (DataPowerMonitorTask) new MetricFetcher.Builder(false)
                .withServer(Collections.singletonMap("uri", "http://localhost:5550/service/mgmt/current"))
                .withConfiguration(c)
                .withSoapMessageUtil(soapMessageUtil)
                .build();
        DataPowerMonitorTask fetcher = Mockito.spy(original);

        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String operation = (String) invocation.getArguments()[0];
                InputStream in = getClass().getResourceAsStream("/output/" + operation + ".xml");
                return soapMessageUtil.getSoapResponseBody(in,
                        operation);
            }
        }).when(fetcher).getResponse(Mockito.anyString(), Mockito.anyString());
        fetcher.fetchMetrics(Arrays.asList("default"), c.getMetricPrefix()+"|");
    }

    @Test
    public void testMetricConverter() {
        final SoapMessageUtil soapMessageUtil = new SoapMessageUtil();
        //Read the YAML and metrics.xml
        AMonitorJob monitorJob = Mockito.mock(AMonitorJob.class);
        MonitorContextConfiguration c = new MonitorContextConfiguration("DataPowerMonitor","Custom Metrics|X|", new File("Dummy"), monitorJob);
        c.setConfigYml("src/main/resources/conf/config.yml");
        c.setMetricXml("src/test/resources/metrics/test-metrics.xml",Stat.Stats.class);
        DataPowerMonitorTask original = (DataPowerMonitorTask) new MetricFetcher.Builder(false)
                .withServer(Collections.singletonMap("uri", "http://localhost:5550/service/mgmt/current"))
                .withConfiguration(c)
                .withSoapMessageUtil(soapMessageUtil)
                .withMetricWriteHelper(Mockito.mock(MetricWriteHelper.class))
                .build();
        DataPowerMonitorTask fetcher = Mockito.spy(original);

        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String operation = (String) invocation.getArguments()[0];
                InputStream in = getClass().getResourceAsStream("/output/" + operation + ".xml");
                return soapMessageUtil.getSoapResponseBody(in,
                        operation);
            }
        }).when(fetcher).getResponse(Mockito.anyString(), Mockito.anyString());
        fetcher.fetchMetrics(Arrays.asList("default"), c.getMetricPrefix());
    }




}