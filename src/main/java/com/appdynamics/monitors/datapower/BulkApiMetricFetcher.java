package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.xml.Xml;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 5/12/15.
 */
public class BulkApiMetricFetcher extends MetricFetcher {
    public static final Logger logger = LoggerFactory.getLogger(BulkApiMetricFetcher.class);

    @Override
    protected void fetchMetrics(List<String> selectedDomains, String serverPrefix) {
        Map<String, Stat> oprMap = new HashMap<String, Stat>();
        Map<String, Stat> sysWidOprMap = new HashMap<String, Stat>();
        Stat[] stats = getStats();
        for (Stat stat : stats) {
            Metric[] metrics = stat.getMetrics();
            if (metrics != null && metrics.length > 0) {
                if ("true".equals(stat.getSystemWide())) {
                    sysWidOprMap.put(stat.getName(), stat);
                } else {
                    oprMap.put(stat.getName(), stat);
                }
            }
        }
        //Get Non system wide metrics for all of the domains
        for (String domain : selectedDomains) {
            String domainPrefix = serverPrefix + domain;
            getResponse(oprMap, domain, domainPrefix);
        }
        //Get the system wide operations any one of the domains
        getResponseForSystemWide(sysWidOprMap, selectedDomains.get(0), StringUtils.trim(serverPrefix, "|"));

    }

    private void getResponseForSystemWide(Map<String, Stat> oprMap, String domain, String prefix) {
        Map<String, Map<String, Stat>> domainStatMap = new HashMap<String, Map<String, Stat>>();
        for (String opr : oprMap.keySet()) {
            Stat stat = oprMap.get(opr);
            String useDomain = stat.getUseDomain();
            if (!StringUtils.hasText(useDomain)) {
                useDomain = domain;
            }
            Map<String, Stat> statMap = domainStatMap.get(useDomain);
            if (statMap == null) {
                statMap = new HashMap<String, Stat>();
                domainStatMap.put(useDomain, statMap);
            }
            statMap.put(opr, stat);
        }
        for (String dom : domainStatMap.keySet()) {
            Map<String, Stat> statMap = domainStatMap.get(dom);
            logger.info("Fetching the operations {} from domain [{}]", statMap.keySet(), dom);
            getResponse(statMap, domain, prefix);
        }
    }

    private void getResponse(Map<String, Stat> oprMap, String domain, String prefix) {
        Map<String, Xml[]> responseMap = getResponse(oprMap.keySet(), domain);
        if (responseMap != null) {
            for (String operation : responseMap.keySet()) {
                Stat stat = oprMap.get(operation);
                if (stat != null) { // This null check is valid only for test cases
                    String statLabel = getStatLabel(prefix, stat);
                    Xml[] response = responseMap.get(operation);
                    response = filter(stat.getFilters(), response);
                    if (response != null && response.length > 0) {
                        extractData(statLabel, stat.getMetrics(), response, stat);
                    }
                }
            }
        }
    }

    protected Map<String, Xml[]> getResponse(Collection<String> operations, String domain) {
        String soapMessage = soapMessageUtil.createSoapMessage(operations, domain);
        String url = UrlBuilder.fromYmlServerConfig(server).build();
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = configuration.getHttpClient();
            logger.debug("The SOAP Request Generated for the domain={} and operation={} is payload={} and url={}"
                    , domain, operations, soapMessage,url);
            HttpPost post = new HttpPost(url);
            StringEntity entity = new StringEntity(soapMessage, ContentType.TEXT_XML);
            post.setEntity(entity);
            response = httpClient.execute(post);
            String responseStr = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                return soapMessageUtil.getSoapResponseBody(responseStr, operations);
            } else {
                logger.error("Error (response code = {}) while fetching the data from absolute url={} and payload={}"
                        , url, soapMessage);
                logger.error("The response code is {} and content is {}", response.getStatusLine(), responseStr);
            }
        } catch (Exception e) {
            String msg = String.format("Error while fetching the data from absolute url=[%s] and payload=[%s]"
                    , url, soapMessage);
            logger.error(msg, e);
            configuration.getMetricWriter().registerError(msg,e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
