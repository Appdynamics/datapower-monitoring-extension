package com.appdynamics.monitors.datapower;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.SimpleHttpClientBuilder;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.util.AggregatedValue;
import com.appdynamics.extensions.util.AggregationType;
import com.appdynamics.extensions.util.Aggregator;
import com.appdynamics.extensions.xml.Xml;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Created by abey.tom on 7/31/15.
 */
public abstract class MetricFetcher implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(MetricFetcher.class);

    protected Map server;
    protected String metricPrefix;
    protected Stat[] metricConf;
    protected Map<String, ?> config;
    protected SoapMessageUtil soapMessageUtil;
    protected DataPowerMonitor metricPrinter;
    protected SimpleHttpClient httpClient;

    public void run() {
        long time = System.currentTimeMillis();
        String uri = (String) server.get("uri");
        try {
            if (!Strings.isNullOrEmpty(uri)) {
                httpClient = buildHttpClient();
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
        } finally {
            if (httpClient != null) {
                logger.debug("Closing the http connection {}", httpClient);
                httpClient.close();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Time taken to run the [{}] on server [{}] is {} ms"
                    , getClass().getName(), uri, (System.currentTimeMillis() - time));
        }
    }

    protected SimpleHttpClient buildHttpClient() {

        HashMap<String, String> argsMap = new HashMap<String, String>();
        argsMap.put(TaskInputArgs.URI, (String) server.get("uri"));
        String username = (String) server.get("username");
        if (username != null) {
            argsMap.put(TaskInputArgs.USER, username);
        }
        String password = (String) server.get("password");
        if (password != null) {
            argsMap.put(TaskInputArgs.PASSWORD, password);
        }
        String passwordEncrypted = (String) server.get("passwordEncrypted");
        if (passwordEncrypted != null) {
            argsMap.put(TaskInputArgs.PASSWORD_ENCRYPTED, passwordEncrypted);
            argsMap.put(TaskInputArgs.ENCRYPTION_KEY, (String) config.get("encryptionKey"));
        }
        SimpleHttpClientBuilder builder = new SimpleHttpClientBuilder();

        Map connection = (Map) config.get("connection");
        if (connection != null) {
            Integer socketTimeout = (Integer) connection.get("socketTimeout");
            if (socketTimeout == null) {
                socketTimeout = 5000;
            }
            Integer connectTimeout = (Integer) connection.get("connectTimeout");
            if (connectTimeout == null) {
                connectTimeout = 5000;
            }
            builder.socketTimeout(socketTimeout).connectionTimeout(connectTimeout);
            String sslProtocol = (String) connection.get("sslProtocol");
            if (sslProtocol != null) {
                argsMap.put("ssl-protocol", sslProtocol);
            }
            logger.debug("Setting the connect timeout to {} and socket timeout to {}", connectTimeout, socketTimeout);
        }
        Map<String, String> proxy = (Map<String, String>) config.get("proxy");
        if (proxy != null) {
            String uri = proxy.get("uri");
            if (uri != null) {
                argsMap.put(TaskInputArgs.PROXY_URI, uri);
            }

            String proxyUser = proxy.get("username");
            if (proxyUser != null) {
                argsMap.put(TaskInputArgs.PROXY_USER, proxyUser);
            }

            String proxyPassword = proxy.get("password");
            if (proxyPassword != null) {
                argsMap.put(TaskInputArgs.PROXY_PASSWORD, proxyPassword);
            }
        }
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (String key : argsMap.keySet()) {
                sb.append(key).append("=");
                if (!key.toLowerCase().contains("password")) {
                    sb.append(argsMap.get(key));
                } else {
                    sb.append("*********");
                }
                sb.append(",");
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            logger.debug("The task args for the http client are {}", sb);
        }
        builder.taskArgs(argsMap);
        return builder.build();
    }


    protected abstract void fetchMetrics(List<String> selectedDomains);

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

    protected Xml[] getResponse(String operation, String domain) {
        String soapMessage = soapMessageUtil.createSoapMessage(operation, domain);
        WebTarget target = httpClient.target();
        Response response = null;
        try {
            logger.debug("The SOAP Request Generated for the domain={} and operation={} is payload={}"
                    , domain, operation, soapMessage);
            response = target.post(soapMessage);
            if (response.getStatus() == 200) {
                return soapMessageUtil.getSoapResponseBody(response.inputStream(), operation);
            } else {
                logger.error("Error while fetching the data from absolute url={}, url={} and payload={}"
                        , target.getAbsoluteUrl(), target.getUrl(), soapMessage);
                logger.error("The response is {}", response.string());
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

        public Builder httpClient(SimpleHttpClient httpClient) {
            task.httpClient = httpClient;
            return this;
        }

        public MetricFetcher build() {
            return task;
        }
    }
}
