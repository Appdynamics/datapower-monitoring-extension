package com.appdynamics.monitors.datapower;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.PathResolver;
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
import com.singularity.ee.agent.systemagent.SystemAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataPowerMonitor extends AManagedMonitor {
    public static final Logger logger = LoggerFactory.getLogger(DataPowerMonitor.class);

    public static final Map<String, String> DEFAULT_ARGS = new HashMap<String, String>() {{
        put("metric-prefix", "Custom Metrics|Data Power");
        put("metric-info-file", "monitors/DataPowerMonitor/metrics.xml");
    }};

    private SoapMessageUtil soapMessageUtil;
    private JAXBContext jaxbContext;

    public DataPowerMonitor() {
        String version = getClass().getPackage().getImplementationTitle();
        String msg = String.format("Using Monitor Version [%s]", version);
        logger.info(msg);
        System.out.println(msg);
        soapMessageUtil = new SoapMessageUtil();
        try {
            jaxbContext = JAXBContext.newInstance(Stat.Stats.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public TaskOutput execute(Map<String, String> argsMap, TaskExecutionContext executionContext) throws TaskExecutionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting the ContextClassLoader of the thread {} from {} to {}"
                    , Thread.currentThread().getName()
                    , Thread.currentThread().getContextClassLoader()
                    , AManagedMonitor.class.getClassLoader());
        }
        Thread.currentThread().setContextClassLoader(AManagedMonitor.class.getClassLoader());
        logger.debug("The raw arguments are {}", argsMap);
        argsMap = ArgumentsValidator.validateArguments(argsMap, DEFAULT_ARGS);
        logger.debug("The validated arguments are {}", argsMap);
        ArgumentsValidator.assertNotEmpty(argsMap, "metric-info-file", "uri");
        SimpleHttpClient client = buildHttpClient(argsMap);
        String metricFilePath = argsMap.get("metric-info-file");
        Stat[] statsInfo = getStatsInfo(metricFilePath);
        if (statsInfo != null && statsInfo.length > 0) {
            List<String> selectedDomains;
            if (Strings.isNullOrEmpty(argsMap.get("domains"))) {
                selectedDomains = getMatchingDomains(client, argsMap);
            } else {
                selectedDomains = Arrays.asList(argsMap.get("domains").split(","));
            }
            if(!selectedDomains.isEmpty()){
                fetchMetrics(statsInfo, client, argsMap, selectedDomains);
            } else{
                logger.error("Cannot match/filter the domains based on the properties 'domains-regex' or 'domains'");
            }
        } else {
            logger.error("The metric file {} seems to be empty or invalid", metricFilePath);
        }
        return new TaskOutput("DataPower Monitor Completed");
    }

    /**
     * Gets the list of domains from the DataPower and then check if it matches with the pattern
     *
     * @param client
     * @param argsMap
     */
    protected List<String> getMatchingDomains(SimpleHttpClient client, Map<String, String> argsMap) {
        List<String> selectedDomains = new ArrayList<String>();
        String domainRegex = argsMap.get("domains-regex");
        if (!Strings.isNullOrEmpty(domainRegex)) {
            Xml[] domains = getResponse(client, "DomainStatus", null);
            if (domains != null) {
                String[] split = domainRegex.split("(?<!\\\\),");
                for (Xml domain : domains) {
                    String domainName = domain.getText("Domain");
                    for (String regex : split) {
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
        } else {
            logger.info("The properties 'domains' and 'domains-regex' are empty. Using the only the 'default' domain");
            selectedDomains.add("default");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("The selected domains are {}", selectedDomains);
        }
        return selectedDomains;
    }

    private SimpleHttpClient buildHttpClient(Map<String, String> argsMap) {
        SimpleHttpClientBuilder builder = SimpleHttpClient.builder(argsMap);
        builder.connectionTimeout(2000).socketTimeout(2000);
        return builder.build();
    }

    protected void fetchMetrics(Stat[] statsInfo, SimpleHttpClient client, Map<String, String> argsMap, List<String> selectedDomains) {
        String metricPrefix = argsMap.get(TaskInputArgs.METRIC_PREFIX);
        for (String domain : selectedDomains) {
            String domainPrefix = metricPrefix + "|" + domain;
            for (Stat stat : statsInfo) {
                String statLabel = stat.getLabel();
                if (StringUtils.hasText(statLabel)) {
                    statLabel = domainPrefix + "|" + StringUtils.trim(statLabel.trim(), "|");
                } else {
                    statLabel = domainPrefix;
                }
                String operation = stat.getName();
                Metric[] metrics = stat.getMetrics();
                if (metrics != null && metrics.length > 0) {
                    Xml[] response = getResponse(client, operation, domain);
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
                        printMetric(label, value, getMetricType(metric, stat));

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
                        printMetric(metricPath, valueStr, getMetricType(metric, stat));
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

    protected Xml[] getResponse(SimpleHttpClient client, String operation, String domain) {
        String soapMessage = soapMessageUtil.createSoapMessage(operation, domain);
        WebTarget target = client.target();
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

    private Stat[] getStatsInfo(String metricFilePath) {
        File dir = PathResolver.resolveDirectory(SystemAgent.class);
        File file = new File(dir, metricFilePath);
        logger.debug("The metric file path is resolved to {}", file.getAbsolutePath());
        if (file.exists()) {
            try {
                return readStatsInfoFile(new FileInputStream(file));
            } catch (FileNotFoundException e) {
            }
        } else {
            logger.warn("The metric file {} does not exist, using the from classpath", file.getAbsolutePath());
            return readStatsInfoFile(getClass().getResourceAsStream("/conf/metrics.xml"));
        }
        return new Stat[0];
    }

    private Stat[] readStatsInfoFile(InputStream inputStream) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Stat.Stats stats = (Stat.Stats) unmarshaller.unmarshal(inputStream);
            if (stats != null) {
                return stats.getStats();
            }
        } catch (JAXBException e) {
            logger.error("Error while unmarshalling the input file", e);
        }
        return new Stat[0];
    }

    protected void printMetric(String metricPath, String value, MetricType metricType) {
        if (metricType == null) {
            printCollectiveObservedCurrent(metricPath, value);
            logger.warn("The metric type was not found for metricPath={} and value={}, defaulting to COLLECTIVE_OBSERVED_CURRENT"
                    , metricPath, value);
        } else {
            switch (metricType) {
                case COLLECTIVE_OBSERVED_AVERAGE:
                    printCollectiveObservedAverage(metricPath, value);
                    break;
                case COLLECTIVE_OBSERVED_CURRENT:
                    printCollectiveObservedCurrent(metricPath, value);
                    break;
                default:
                    logger.error("Unknown metric type {}. Ignoring the metric={} and value={}",
                            metricType, metricPath, value);
                    break;
            }
        }
    }

    protected void printCollectiveObservedCurrent(String metricName, String metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    protected void printCollectiveObservedAverage(String metricName, String metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    public void printMetric(String metricPath, String metricValue, String aggregation, String timeRollup, String cluster) {
        MetricWriter metricWriter = getMetricWriter(metricPath,
                aggregation,
                timeRollup,
                cluster
        );
        if (metricValue != null) {
            metricWriter.printMetric(metricValue.toString());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Metric [" + aggregation + "/" + timeRollup + "/" + cluster
                    + "] metric = " + metricPath + " = " + metricValue);
        }
    }


}
