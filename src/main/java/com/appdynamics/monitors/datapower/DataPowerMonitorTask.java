package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.util.AggregatedValue;
import com.appdynamics.extensions.util.AggregationType;
import com.appdynamics.extensions.util.Aggregator;
import com.appdynamics.extensions.xml.Xml;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 5/12/15.
 */
public class DataPowerMonitorTask implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(DataPowerMonitorTask.class);

    private Map server;
    private String metricPrefix;
    private Stat[] metricConf;
    private Map<String, ?> config;
    private SoapMessageUtil soapMessageUtil;
    private DataPowerMonitor metricPrinter;
    private CloseableHttpClient httpClient;

    protected DataPowerMonitorTask() {
    }

    public void run() {
        Thread.currentThread().setContextClassLoader(DataPowerMonitor.class.getClassLoader());
        long time = System.currentTimeMillis();
        String uri = (String) server.get("uri");
        try {
            if (!Strings.isNullOrEmpty(uri)) {
                String displayName = (String) server.get("displayName");
                if (!Strings.isNullOrEmpty(displayName)) {
                    metricPrefix = metricPrefix + displayName + "|";
                }
                logger.debug("Fetching metrics for the server uri [{}], metricPrefix =[{}]", uri, metricPrefix);
                List<String> selectedDomains = getSelectedDomains();
                if (!selectedDomains.isEmpty()) {
                    fetchMetrics(selectedDomains);
                } else {
                    logger.error("Cannot match/filter the domains based on the properties 'domains-regex' or 'domains'");
                }
            } else {
                logger.error("The url is empty for the server {}", server);
            }
        } catch (Exception e) {
            logger.error("Exception while running the DataPower task in the server " + uri, e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Time taken to run the task on server [{}] is {} ms", uri, (System.currentTimeMillis() - time));
        }
    }

    protected void fetchMetrics(List<String> selectedDomains) {
        for (String domain : selectedDomains) {
            String domainPrefix = metricPrefix + domain;
            for (Stat stat : metricConf) {
                String statLabel = stat.getLabel();
                if (StringUtils.hasText(statLabel)) {
                    statLabel = domainPrefix + "|" + StringUtils.trim(statLabel.trim(), "|");
                } else {
                    statLabel = domainPrefix;
                }
                String operation = stat.getName();
                Metric[] metrics = stat.getMetrics();
                if (metrics != null && metrics.length > 0) {
                    Xml[] response = getResponse(operation, domain);
                    if (response != null) {
                        extractData(statLabel, metrics, response, stat);
                    }
                }
            }
        }
    }

    private void extractData(String metricPrefix, Metric[] metrics, Xml[] response, Stat stat) {
        Aggregator<Metric> aggregator = new Aggregator<Metric>();
        for (Xml xml : response) {
            for (Metric metric : metrics) {
                String valueXpath = metric.getValueXpath();
                if (StringUtils.hasText(valueXpath)) {
                    String value = xml.getText(valueXpath);
                    if (StringUtils.hasText(value)) {
                        if (metric.getAggregateLabel() != null) {
                            aggregator.add(metric, value);
                        }
                        value = multiply(value, metric.getMultiplier());
                        String label = getLabel(xml, metric);
                        if (StringUtils.hasText(label)) {
                            label = metricPrefix + "|" + StringUtils.trim(label, "|");
                        } else {
                            label = metricPrefix + "|" + valueXpath.replace("\\W", "");
                        }
                        metricPrinter.printMetric(label, value, getMetricType(metric, stat));
                    } else {
                        logger.warn("The value fetched by xpath={} is empty for the xml={}", valueXpath, xml);
                    }
                } else {
                    logger.error("The value-xpath is null for the metric {}", metric);
                }
            }
        }
        if (!aggregator.isEmpty()) {
            for (Metric metric : metrics) {
                AggregatedValue aggregate = aggregator.get(metric);
                if (aggregate != null) {
                    BigDecimal value = null;
                    if (AggregationType.AVERAGE.equals(metric.getAggregationType())) {
                        value = aggregate.getAverage();
                    } else {
                        value = aggregate.getSum();
                    }
                    if (value != null) {
                        String label = StringUtils.trim(metric.getAggregateLabel(), "|");
                        String metricPath = metricPrefix + "|" + label;
                        if (metric.getMultiplier() != null) {
                            value = value.multiply(metric.getMultiplier());
                        }
                        String valueStr = value.setScale(0, RoundingMode.HALF_UP).toString();
                        metricPrinter.printMetric(metricPath, valueStr, getMetricType(metric, stat));
                    }
                }
            }
        }
    }

    private MetricType getMetricType(Metric metric, Stat stat) {
        if (metric.getMetricType() != null) {
            return metric.getMetricType();
        } else if (stat.getMetricType() != null) {
            return stat.getMetricType();
        } else {
            return null;
        }
    }

    private String multiply(String value, BigDecimal multiplier) {
        if (StringUtils.hasText(value)) {
            if (multiplier != null) {
                BigDecimal multiply = new BigDecimal(value).multiply(multiplier);
                return multiply.setScale(0, RoundingMode.HALF_UP).toString();
            } else {
                return new BigDecimal(value).setScale(0, RoundingMode.HALF_UP).toString();
            }
        }
        return null;
    }

    private String getLabel(Xml xml, Metric metric) {
        String label = "";
        if (metric.getLabel() != null) {
            label = metric.getLabel();
        } else if (metric.getLabelXpath() != null) {
            label = xml.getText(metric.getLabelXpath());
        }
        if (metric.getLabelPrefix() != null) {
            label = metric.getLabelPrefix() + label;
        }
        if (metric.getLabelSuffix() != null) {
            label = label + metric.getLabelSuffix();
        }
        return label.trim();
    }

    private List<String> getSelectedDomains() {
        List domains = (List) server.get("domains");
        if (domains == null || domains.isEmpty()) {
            List<String> domainsRegex = (List<String>) server.get("domainsRegex");
            if (domainsRegex != null && !domainsRegex.isEmpty()) {
                domains = getMatchingDomains(domainsRegex);
            } else {
                logger.debug("The properties 'domains' and 'domainsRegex' are empty. Using the only the 'default' domain");
                domains = Collections.singletonList("default");
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("The selected domains are {}", domains);
        }
        return domains;
    }

    /**
     * Gets the list of domains from the DataPower and then check if it matches with the pattern
     */
    protected List<String> getMatchingDomains(List<String> domainsRegex) {
        List<String> selectedDomains = new ArrayList<String>();
        Xml[] domains = getResponse("DomainStatus", null);
        if (domains != null) {
            for (Xml domain : domains) {
                String domainName = domain.getText("Domain");
                for (String regex : domainsRegex) {
                    if (domainName.matches(regex)) {
                        logger.debug("The match of regex {} with the domain name {} returned true", regex, domainName);
                        selectedDomains.add(domainName);
                        break;
                    } else {
                        logger.debug("The match of regex {} with the domain name {} returned false", regex, domainName);
                    }
                }
            }
        } else {
            logger.warn("No domains were discovered, adding only the 'default' domain");
            selectedDomains.add("default");
        }

        return selectedDomains;
    }

    protected Xml[] getResponse(final String operation, String domain) {
        final String uri = (String) server.get("uri");
        final String soapMessage = soapMessageUtil.createSoapMessage(operation, domain);
        try {
            logger.debug("The SOAP Request Generated for the domain=[{}] and operation=[{}] is payload=[{}]"
                    , domain, operation, soapMessage);
            HttpPost post = new HttpPost(uri);
            logger.debug("The resource is {}", uri);
            RequestConfig.Builder config =  RequestConfig.custom();

            post.setEntity(new StringEntity(soapMessage, ContentType.APPLICATION_XML));
            return httpClient.execute(post, new ResponseHandler<Xml[]>() {
                public Xml[] handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    if (response.getStatusLine().getStatusCode() == 200) {
                        return soapMessageUtil.getSoapResponseBody(response.getEntity().getContent(), operation);
                    } else {
                        logger.error("Error while fetching the data from absolute url = [{}] and payload = [{}]"
                                , uri, soapMessage);
                        logger.error("The response status is [{}] and data is [{}]", response.getStatusLine()
                                , EntityUtils.toString(response.getEntity()));
                        return new Xml[0];
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Error while fetching the data from url [{}] and payload = [{}]"
                    , uri, soapMessage);
            logger.error("", e);
        }
        return new Xml[0];
    }

    public static class Builder {
        private DataPowerMonitorTask task;

        public Builder() {
            task = new DataPowerMonitorTask();
        }

        public Builder server(Map server) {
            task.server = server;
            return this;
        }

        public Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }


        public Builder metricConfig(Stat[] metricConfig) {
            task.metricConf = metricConfig;
            return this;
        }

        public Builder config(Map<String, ?> config) {
            task.config = config;
            return this;
        }

        public Builder soapMessageUtil(SoapMessageUtil soapMessageUtil) {
            task.soapMessageUtil = soapMessageUtil;
            return this;
        }

        public Builder metricWriter(DataPowerMonitor dataPowerMonitor) {
            task.metricPrinter = dataPowerMonitor;
            return this;
        }

        public Builder httpClient(CloseableHttpClient httpClient) {
            task.httpClient = httpClient;
            return this;
        }

        public DataPowerMonitorTask build() {
            return task;
        }
    }


}
