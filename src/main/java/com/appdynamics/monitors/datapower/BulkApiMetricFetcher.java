package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.xml.Xml;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected void fetchMetrics(List<String> selectedDomains) {
        Map<String, Stat> operationMap = new HashMap<String, Stat>();
        for (Stat stat : metricConf) {
            Metric[] metrics = stat.getMetrics();
            if (metrics != null && metrics.length > 0) {
                operationMap.put(stat.getName(), stat);
            }
        }
        for (String domain : selectedDomains) {
            String domainPrefix = metricPrefix + domain;
            Map<String, Xml[]> responseMap = getResponse(operationMap.keySet(), domain);
            for (String operation : responseMap.keySet()) {
                Stat stat = operationMap.get(operation);
                String statLabel = getStatLabel(domainPrefix, stat);
                Xml[] response = responseMap.get(operation);
                extractData(statLabel, stat.getMetrics(), response, stat);
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
