package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.yml.YmlReader;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.appdynamics.monitors.util.TestHelper;
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
import java.util.Map;

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
        DataPowerMonitor monitor = Mockito.spy(new DataPowerMonitor());
        final SoapMessageUtil soapMessageUtil = new SoapMessageUtil();
        //Read the YAML and metrics.xml
        Map<String, ?> config = YmlReader.readFromFileAsMap(new File(getClass().getResource("/conf/config.yml").getFile()));
        Stat[] metricConfig = null;
                //monitor.readStatsInfoFile(getClass().getResourceAsStream("/metrics/test-metrics.xml"));

        MonitorConfiguration c = new MonitorConfiguration("Custom Metrics|X|");
        c.setConfigYml("src/main/resources/conf/config.yml");
        c.setMetricsXml("src/test/resources/metrics/test-metrics.xml",Stat.Stats.class);
        c.setMetricWriter(Mockito.mock(MetricWriteHelper.class));
        //Create the Task with builder
        DataPowerMonitorTask original = (DataPowerMonitorTask) new MetricFetcher.Builder(false)
                .server(Collections.singletonMap("uri", "http://localhost:5550/service/mgmt/current"))
                .configuration(c)
                .soapMessageUtil(soapMessageUtil)
                .metricWriter(Mockito.mock(DataPowerMonitor.class))
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
        DataPowerMonitor monitor = Mockito.spy(new DataPowerMonitor());
        final SoapMessageUtil soapMessageUtil = new SoapMessageUtil();
        //Read the YAML and metrics.xml
        Map<String, ?> config = YmlReader.readFromFileAsMap(new File(getClass().getResource("/conf/config.yml").getFile()));
        Stat[] metricConfig = null;
//                monitor.readStatsInfoFile(getClass().getResourceAsStream("/metrics/test-metric-converter.xml"));
        MonitorConfiguration c = new MonitorConfiguration("Custom Metrics|X|");
        c.setConfigYml("src/main/resources/conf/config.yml");
        c.setMetricsXml("src/test/resources/metrics/test-metrics.xml",Stat.Stats.class);
        c.setMetricWriter(Mockito.mock(MetricWriteHelper.class));
        //Create the Task with builder
        DataPowerMonitorTask original = (DataPowerMonitorTask) new MetricFetcher.Builder(false)
                .server(Collections.singletonMap("uri", "http://localhost:5550/service/mgmt/current"))
                .configuration(c)
                .soapMessageUtil(soapMessageUtil)
                .metricWriter(Mockito.mock(DataPowerMonitor.class))
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