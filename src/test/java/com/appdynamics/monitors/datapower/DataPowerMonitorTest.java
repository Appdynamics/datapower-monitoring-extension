package com.appdynamics.monitors.datapower;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.appdynamics.extensions.xml.Xml;
import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
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

    public DataPowerMonitorTest() {
        put("DP|test|Network|eth0|Outgoing Packets/sec", "1", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|eth1|Outgoing Packets/sec", "2", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|eth2|Outgoing Packets/sec", "3", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|eth3|Outgoing Packets/sec", "4", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Network|Total Outgoing Packets/sec", "10", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Used %", "21", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Total (MB)", numberToString(3368445D/1024D), "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Used (MB)", numberToString(717004D/1024D), "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Free (MB)", numberToString(2651441D/1024D), "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|System|Memory|Requested (MB)", numberToString(722628D/1024D), "COLLECTIVE_OBSERVED_AVERAGE");

        //HTTPMeanTransactionTime
        put("DP|test|Transactions|helloworld_xmlfw|Average Response Time (ms)","8", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Transactions|userws_proxy|Average Response Time (ms)","17", "COLLECTIVE_OBSERVED_AVERAGE");
        put("DP|test|Transactions|Average Response Time (ms)","12", "COLLECTIVE_OBSERVED_AVERAGE");
        //HTTPTransactions
        put("DP|test|Transactions|helloworld_xmlfw|Calls per Minute","180", "COLLECTIVE_OBSERVED_CURRENT");
        put("DP|test|Transactions|userws_proxy|Calls per Minute","420", "COLLECTIVE_OBSERVED_CURRENT");
        put("DP|test|Transactions|wsproxy|Calls per Minute","0", "COLLECTIVE_OBSERVED_CURRENT");
        put("DP|test|Transactions|Calls per Minute","600", "COLLECTIVE_OBSERVED_CURRENT");

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

//    @Test
    public void test() throws TaskExecutionException, JAXBException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("uri", "https://172.16.153.100:5550/service/mgmt/current");
        map.put(TaskInputArgs.USER, "admin");
        map.put(TaskInputArgs.PASSWORD, "welcome");
        new DataPowerMonitor().execute(map, null);
    }

    @Test(expected = ArgumentsValidator.InvalidInputException.class)
    public void testMandatoryArgumentURI() throws TaskExecutionException, JAXBException {
        HashMap<String, String> map = new HashMap<String, String>();
        new DataPowerMonitor().execute(map, null);
    }



    @Test(expected = ArgumentsValidator.InvalidInputException.class)
    public void testMandatoryArgumentMetricInfoFile() throws TaskExecutionException, JAXBException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("uri", "https://172.16.153.100:5550/service/mgmt/current");
        map.put("metric-info-file", "");
        new DataPowerMonitor().execute(map, null);
    }

    @Test
    public void testDefaultArgs() throws TaskExecutionException, JAXBException {
        DataPowerMonitor monitor = new DataPowerMonitor();
        DataPowerMonitor spy = Mockito.spy(monitor);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock inv) throws Throwable {
                Map<String,String> argsMap = (Map<String, String>) inv.getArguments()[2];
                Assert.assertEquals("default",argsMap.get("domains"));
                Assert.assertEquals("Custom Metrics|Data Power",argsMap.get("metric-prefix"));
                Assert.assertEquals("monitors/DataPowerMonitor/metrics.xml",argsMap.get("metric-info-file"));
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }).when(spy).fetchMetrics(Mockito.any(Stat[].class),Mockito.any(SimpleHttpClient.class),Mockito.any(Map.class));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("uri", "https://172.16.153.100:5550/service/mgmt/current");
        spy.execute(map, null);
    }

    @Test
    public void testAll() throws TaskExecutionException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("uri", "https://172.16.153.100:5550/service/mgmt/current");
        map.put("metric-prefix", "DP||");
        map.put("domains", "test");
        DataPowerMonitor monitor = Mockito.spy(new DataPowerMonitor());
//        Mockito.doAnswer(new Answer<Xml[]>() {
//            public Xml[] answer(InvocationOnMock invocationOnMock) throws Throwable {
//                String operation = (String) invocationOnMock.getArguments()[1];
//                return getResponse(operation);
//            }
//        }).when(monitor).getResponse(Mockito.any(SimpleHttpClient.class), Mockito.anyString(), Mockito.anyString());
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock inv) throws Throwable {
                Object[] args = inv.getArguments();
                String path = (String) args[0];
                String value = (String) args[1];
                String type;
                if(args[2]!=null){
                    type = ((MetricType) args[2]).name();
                } else{
                    type = MetricType.COLLECTIVE_OBSERVED_CURRENT.name();
                }
                logger.info("For the path {} the value {} and type {} is expected", path, value, type);
                Map<String, String> map = expectedDataMap.get(path);
                if (map != null) {
                    String val = map.get(value);
                    if (val == null) {
                        Assert.assertNotNull(val);
                    } else {
                        Assert.assertEquals(val, type);
                    }
                } else {
                    logger.error("Looks like the path {} is NOT expected", path);
                    Assert.assertNotNull(map);
                }
                expectedDataMap.remove(path);
                return null;
            }
        }).when(monitor).printMetric(Mockito.anyString(), Mockito.anyString(), Mockito.any(MetricType.class));
        monitor.execute(map, null);
        logger.info("The contents of expectedDataMap is {}", expectedDataMap);
        Assert.assertTrue(expectedDataMap.isEmpty());
    }

    private Xml[] getResponse(String operation) {
        InputStream in = getClass().getResourceAsStream("/output/" + operation + ".xml");
        if (in != null) {
            return new SoapMessageUtil().getSoapResponseBody(in, operation);
        } else {
            return null;
        }
    }
}
