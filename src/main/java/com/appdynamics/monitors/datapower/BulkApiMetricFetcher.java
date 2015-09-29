package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.xml.Xml;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by abey.tom on 5/12/15.
 */
public class BulkApiMetricFetcher extends MetricFetcher {
    public static final Logger logger = LoggerFactory.getLogger(BulkApiMetricFetcher.class);

    @Override
    protected void fetchMetrics(List<String> selectedDomains) {
        Map<String, Stat> oprMap = new HashMap<String, Stat>();
        Map<String, Stat> sysWidOprMap = new HashMap<String, Stat>();
        for (Stat stat : metricConf) {
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
            String domainPrefix = metricPrefix + domain;
            getResponse(oprMap, domain, domainPrefix);
        }
        //Get the system wide operations any one of the domains
        getResponseForSystemWide(sysWidOprMap, selectedDomains.get(0), StringUtils.trim(metricPrefix, "|"));

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
        WebTarget target = httpClient.target();
        Response response = null;
        try {
            logger.debug("The SOAP Request Generated for the domain={} and operation={} is payload={}"
                    , domain, operations, soapMessage);
            response = target.post(soapMessage);
            String responseStr = response.string();
            if (response.getStatus() == 200 && !Strings.isNullOrEmpty(responseStr)) {
                return soapMessageUtil.getSoapResponseBody(responseStr, operations);
            } else {
                logger.error("Error (response code = {}) while fetching the data from absolute url={}, url={} and payload={}"
                        , target.getAbsoluteUrl(), target.getUrl(), soapMessage);
                logger.error("The response code is {} and content is {}", response.getStatus(), responseStr);
            }
        } catch (Exception e) {
            logger.error("Error while fetching the data from absolute url={}, url={} and payload={}"
                    , target.getAbsoluteUrl(), target.getUrl(), soapMessage);
            logger.error("", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
}
