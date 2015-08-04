package com.appdynamics.monitors.datapower;

import com.appdynamics.monitors.util.TestHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
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
        List<String> domains = monitor.getMatchingDomains(Arrays.asList("Domain.*","Not.*"));
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

}