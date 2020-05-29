/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.datapower.config.MetricConfig;
import com.appdynamics.extensions.datapower.config.Stat;
import com.appdynamics.extensions.datapower.util.Constants;
import com.appdynamics.extensions.datapower.util.SoapMessageUtil;
import com.appdynamics.extensions.datapower.util.Xml;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by abey.tom on 7/31/15.
 */
public abstract class MetricFetcher implements Runnable {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricFetcher.class);

    protected Map server;
    protected SoapMessageUtil soapMessageUtil;
    protected MonitorContextConfiguration configuration;
    protected MetricWriteHelper metricWriteHelper;
    private BigInteger heartBeatValue = BigInteger.ZERO;
    private ObjectMapper objectMapper = new ObjectMapper();

    public void run() {
        long time = System.currentTimeMillis();
        String uri = (String) server.get("uri");
        List<Metric> metrics = Lists.newArrayList();
        try {
            if (!Strings.isNullOrEmpty(uri)) {
                String displayName = (String) server.get(Constants.DISPLAY_NAME);
                String serverPrefix;
                if (!Strings.isNullOrEmpty(displayName)) {
                    serverPrefix = configuration.getMetricPrefix() + Constants.METRIC_SEPARATOR + displayName + Constants.METRIC_SEPARATOR;
                } else {
                    serverPrefix = configuration.getMetricPrefix() + Constants.METRIC_SEPARATOR;
                }
                logger.debug("Fetching metrics for the server uri [{}], metricPrefix =[{}]", uri, serverPrefix);
                List<String> selectedDomains = getSelectedDomains();
                if (!selectedDomains.isEmpty()) {
                    metrics = fetchMetrics(selectedDomains, serverPrefix);
                    if(metrics.size() > 0)
                       heartBeatValue = BigInteger.ONE;
                } else {
                    logger.debug("Cannot match/filter the domains based on the properties 'domains-regex' or 'domains'");
                }
            } else {
                logger.error("The url is empty for the server {}", server);
            }
        } catch (Exception e) {
            String msg = "Exception while running the DataPower task in the server " + uri;
            logger.error(msg, e);
        }finally {
            Metric heartBeat = new Metric("HeartBeat", String.valueOf(heartBeatValue), configuration.getMetricPrefix() + Constants.METRIC_SEPARATOR + Constants.HEARTBEAT);
            metrics.add(heartBeat);
            metricWriteHelper.transformAndPrintMetrics(metrics);
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

    protected abstract List<Metric> fetchMetrics(List<String> selectedDomains, String serverPrefix);

    protected String getLabel(Xml xml, MetricConfig metricConfig) {
        String label = "";
        if (metricConfig.getLabel() != null) {
            label = metricConfig.getLabel();
        } else if (metricConfig.getLabelXpath() != null) {
            String labelXpath = metricConfig.getLabelXpath();
            String delim = StringUtils.hasText(metricConfig.getLabelDelim()) ? metricConfig.getLabelDelim() : "_";
            if (labelXpath.contains(",")) {
                String[] split = labelXpath.split(",");
                StringBuilder sb = new StringBuilder();
                for (String xpath : split) {
                    String text = xml.getText(xpath);
                    if (StringUtils.hasText(text)) {
                        sb.append(text).append(delim);
                    } else {
                        logger.debug("The xpath {} to get the label returned nothing", xpath);
                    }
                }
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                label = sb.toString();
            } else {
                label = xml.getText(metricConfig.getLabelXpath());
            }
        }
        if (metricConfig.getLabelPrefix() != null) {
            label = metricConfig.getLabelPrefix() + label;
        }
        if (metricConfig.getLabelSuffix() != null) {
            label = label + metricConfig.getLabelSuffix();
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

    protected void extractData(String metricPrefix, MetricConfig[] metricConfigs, Xml[] response, Stat stat, List<Metric> metricsList) {
        for (Xml xml : response) {
            for (MetricConfig metricConfig : metricConfigs) {
                String valueXpath = metricConfig.getValueXpath();
                if (StringUtils.hasText(valueXpath)) {
                    String value = xml.getText(valueXpath);
                    if (StringUtils.hasText(value)) {
                        String label = getLabel(xml, metricConfig);
                        if (StringUtils.hasText(label)) {
                            label = metricPrefix + "|" + StringUtils.trim(label, "|");
                        } else {
                            valueXpath = valueXpath.replace("\\W", "");
                            label = metricPrefix + "|" + valueXpath;
                        }
                        Metric metric = new Metric(valueXpath, value, label, objectMapper.convertValue(metricConfig, Map.class));
                        metricsList.add(metric);
                    } else {
                        logger.debug("The value fetched by xpath={} is empty for the xml={}", valueXpath, xml);
                    }
                } else {
                    logger.error("The value-xpath is null for the metric {}", metricConfig);
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
                    logger.debug("The filter {} didn't return any node", filter);
                }
            }
            return filtered.toArray(new Xml[]{});
        } else {
            return response;
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

        public Builder withServer(Map server) {
            task.server = server;
            return this;
        }

        public Builder withSoapMessageUtil(SoapMessageUtil soapMessageUtil) {
            task.soapMessageUtil = soapMessageUtil;
            return this;
        }

        public Builder withConfiguration(MonitorContextConfiguration configuration) {
            task.configuration = configuration;
            return this;
        }

        public Builder withMetricWriteHelper(MetricWriteHelper metricWriteHelper){
            task.metricWriteHelper = metricWriteHelper;
            return this;
        }

        public MetricFetcher build() {
            return task;
        }
    }
}
