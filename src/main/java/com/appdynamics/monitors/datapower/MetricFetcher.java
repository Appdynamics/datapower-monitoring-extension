/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.metrics.AggregatedValue;
import com.appdynamics.extensions.metrics.AggregationType;
import com.appdynamics.extensions.metrics.Aggregator;
import com.appdynamics.extensions.util.StringUtils;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.appdynamics.monitors.util.Xml;
import com.google.common.base.Strings;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 7/31/15.
 */
public abstract class MetricFetcher implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(MetricFetcher.class);

    protected Map server;
    protected SoapMessageUtil soapMessageUtil;
    protected MonitorContextConfiguration configuration;
    protected DataPowerMonitor metricPrinter;

    public void run() {
        long time = System.currentTimeMillis();
        String uri = (String) server.get("uri");
        try {
            if (!Strings.isNullOrEmpty(uri)) {
                String displayName = (String) server.get("displayName");
                String serverPrefix;
                if (!Strings.isNullOrEmpty(displayName)) {
                    serverPrefix = configuration.getMetricPrefix() + "|" + displayName + "|";
                } else {
                    serverPrefix = configuration.getMetricPrefix() + "|";
                }
                logger.debug("Fetching metrics for the server uri [{}], metricPrefix =[{}]", uri, serverPrefix);
                List<String> selectedDomains = getSelectedDomains();
                if (!selectedDomains.isEmpty()) {
                    fetchMetrics(selectedDomains, serverPrefix);
                } else {
                    logger.error("Cannot match/filter the domains based on the properties 'domains-regex' or 'domains'");
                }
            } else {
                logger.error("The url is empty for the server {}", server);
            }
        } catch (Exception e) {
            String msg = "Exception while running the DataPower task in the server " + uri;
            logger.error(msg, e);
//            configuration.getMetricWriter().registerError(msg, e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Time taken to run the [{}] on server [{}] is {} ms"
                    , getClass().getName(), uri, (System.currentTimeMillis() - time));
        }
    }

    protected Stat[] getStats() {
        Stat.Stats wrapper = (Stat.Stats) this.configuration.getMetricsXml();
        if (wrapper != null) {
            return wrapper.getStats();
        } else {
            return null;
        }
    }

    protected abstract void fetchMetrics(List<String> selectedDomains, String serverPrefix);

    protected MetricType getMetricType(Metric metric, Stat stat) {
        if (metric.getMetricType() != null) {
            return metric.getMetricType();
        } else if (stat.getMetricType() != null) {
            return stat.getMetricType();
        } else {
            return null;
        }
    }

    protected String multiply(String value, BigDecimal multiplier) {
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

    protected String getLabel(Xml xml, Metric metric) {
        String label = "";
        if (metric.getLabel() != null) {
            label = metric.getLabel();
        } else if (metric.getLabelXpath() != null) {
            String labelXpath = metric.getLabelXpath();
            String delim = StringUtils.hasText(metric.getLabelDelim()) ? metric.getLabelDelim() : "_";
            if (labelXpath.contains(",")) {
                String[] split = labelXpath.split(",");
                StringBuilder sb = new StringBuilder();
                for (String xpath : split) {
                    String text = xml.getText(xpath);
                    if (StringUtils.hasText(text)) {
                        sb.append(text).append(delim);
                    } else {
                        logger.warn("The xpath {} to get the label returned nothing", xpath);
                    }
                }
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                label = sb.toString();
            } else {
                label = xml.getText(metric.getLabelXpath());
            }
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

    protected Xml[] getResponse(String operation, String domain) {
        String soapMessage = soapMessageUtil.createSoapMessage(operation, domain);
        String url = UrlBuilder.fromYmlServerConfig(server).build();
            CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = configuration.getContext().getHttpClient();
            logger.debug("The SOAP Request Generated for the domain={} and operation={} is payload={} and url={}"
                    , domain, operation, soapMessage, url);
            HttpPost post = new HttpPost(url);
            StringEntity entity = new StringEntity(soapMessage, ContentType.TEXT_XML);
            post.setEntity(entity);
            response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() == 200) {
                return soapMessageUtil.getSoapResponseBody(response.getEntity().getContent(), operation);
            } else {
                logger.error("Error while fetching the data from absolute url={} and payload={}"
                        , url, soapMessage);
                logger.error("The response is {}", EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            String msg = String.format("Error while fetching the data from url=[%s] and payload=[%s]",
                    url, soapMessage);
            logger.error(msg, e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
        }
        return new Xml[0];
    }

    protected String getStatLabel(String domainPrefix, Stat stat) {
        String statLabel = stat.getLabel();
        if (StringUtils.hasText(statLabel)) {
            statLabel = domainPrefix + "|" + StringUtils.trim(statLabel.trim(), "|");
        } else {
            statLabel = domainPrefix;
        }
        return statLabel;
    }

    protected void extractData(String metricPrefix, Metric[] metrics, Xml[] response, Stat stat) {
        Aggregator<Metric> aggregator = new Aggregator<Metric>();
        for (Xml xml : response) {
            for (Metric metric : metrics) {
                Map<String, String> converterMap = getConverterMap(metric);
                String valueXpath = metric.getValueXpath();
                if (StringUtils.hasText(valueXpath)) {
                    String value = xml.getText(valueXpath);
                    if (StringUtils.hasText(value)) {
                        value = convertIfNeeded(value, converterMap, metric);
                        value = multiply(value, metric.getMultiplier());
                        if (metric.getAggregateLabel() != null) {
                            aggregator.add(metric, value);
                        }
                        String label = getLabel(xml, metric);
                        if (StringUtils.hasText(label)) {
                            label = metricPrefix + "|" + StringUtils.trim(label, "|");
                        } else {
                            label = metricPrefix + "|" + valueXpath.replace("\\W", "");
                        }
                        metricPrinter.printMetric(label, value, getMetricType(metric, stat));
                    } else {
                        BulkApiMetricFetcher.logger.warn("The value fetched by xpath={} is empty for the xml={}", valueXpath, xml);
                    }
                } else {
                    BulkApiMetricFetcher.logger.error("The value-xpath is null for the metric {}", metric);
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

    protected Xml[] filter(String[] filters, Xml[] response) {
        if (filters != null && filters.length > 0 && response != null && response.length > 0) {
            Xml parentNode = response[0].getParent();
            List<Xml> filtered = new ArrayList<Xml>();
            for (String filter : filters) {
                Xml xml = parentNode.getXmlFromXpath(filter);
                if (xml != null) {
                    filtered.add(xml);
                } else {
                    logger.warn("The filter {} didn't return any node", filter);
                }
            }
            return filtered.toArray(new Xml[]{});
        } else {
            return response;
        }
    }

    private String convertIfNeeded(String value, Map<String, String> converterMap, Metric metric) {
        if (converterMap != null) {
            String converted = converterMap.get(value);
            if (StringUtils.hasText(converted)) {
                return converted;
            } else if (converterMap.containsKey("$default")) {
                return converterMap.get("$default");
            } else {
                logger.error("For the {}, the converter map {} has no value for [{}]"
                        , metric, converterMap, value);
                return value;
            }
        }
        return value;
    }


    private Map<String, String> getConverterMap(Metric metric) {
        MetricConverter[] converters = metric.getConverters();
        if (converters != null && converters.length > 0) {
            Map<String, String> map = new HashMap<String, String>();
            for (MetricConverter converter : converters) {
                map.put(converter.getLabel(), converter.getValue());
            }
            return map;
        } else {
            return null;
        }
    }

    public static class Builder {
        private MetricFetcher task;

        public Builder(boolean useBulkApi) {
            if (useBulkApi) {
                task = new BulkApiMetricFetcher();
            } else {
                task = new DataPowerMonitorTask();
            }
        }

        public Builder server(Map server) {
            task.server = server;
            return this;
        }

        public Builder soapMessageUtil(SoapMessageUtil soapMessageUtil) {
            task.soapMessageUtil = soapMessageUtil;
            return this;
        }

        public Builder configuration(MonitorContextConfiguration configuration) {
            task.configuration = configuration;
            return this;
        }

        public MetricFetcher build() {
            return task;
        }

        public Builder metricWriter(DataPowerMonitor dataPowerMonitor) {
            task.metricPrinter = dataPowerMonitor;
            return this;
        }
    }
}
