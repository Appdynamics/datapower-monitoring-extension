/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.util.SoapMessageUtil;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataPowerMonitor extends ABaseMonitor {
    public static final Logger logger = LoggerFactory.getLogger(DataPowerMonitor.class);
    private static final String METRIC_PREFIX = "Custom Metrics|Data Power|";

    private SoapMessageUtil soapMessageUtil;
    private MonitorContextConfiguration configuration;
    private MetricWriteHelper metricWriter; //TODO: remove it


    public DataPowerMonitor() {
        String version = getClass().getPackage().getImplementationTitle();
        String msg = String.format("Using Monitor Version [%s]", version);
        logger.info(msg);
        System.out.println(msg);
        soapMessageUtil = new SoapMessageUtil();
    }

    protected String getDefaultMetricPrefix() {
        return null;
    }

    public String getMonitorName() {
        return null;
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        configuration = getContextConfiguration();
        metricWriter = tasksExecutionServiceProvider.getMetricWriteHelper(); //TODO: remove it
        Map<String, ?> config = configuration.getConfigYml();
        Stat.Stats metricConfig = (Stat.Stats) configuration.getMetricsXml();
        if (config != null && metricConfig != null && metricConfig.getStats() != null) {
            List<Map> servers = (List) config.get("servers");
            if (servers != null && !servers.isEmpty()) {
                for (Map server : servers) {
                    MetricFetcher task = createTask(server);
                    configuration.getContext().getExecutorService().execute("Datapower-" + server.get("displayName"), task);
                }
            } else {
                logger.error("There are no servers configured");
            }
        } else {
            if (config == null) {
                logger.error("The config.yml is not loaded due to previous errors.The task will not run");
            }
            if (metricConfig == null) {
                logger.error("The metrics.xml is not loaded due to previous errors.The task will not run");
            }
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        Map<String, ?> config = configuration.getConfigYml();
        AssertUtils.assertNotNull(config, "The config is not loaded due to previous error");
        List<Map<String, ?>> servers = (List<Map<String, ?>>) config.get("servers");
        AssertUtils.assertNotNull(servers, "The 'instances' section in config.yml is not initialised");
        return servers;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        configuration = getContextConfiguration();
        logger.info("initializing metric.xml file");
        configuration.setMetricXml(args.get("metric-file"), Stat.Stats.class);
    }

    private boolean useBulkApi(Map server) {
        Boolean useLegacyBulkApi = (Boolean) server.get("useBulkApi");
        boolean useBulkApi;
        if (Boolean.TRUE.equals(useLegacyBulkApi)) {
            useBulkApi = true;
        } else {
            useBulkApi = false;
        }
        return useBulkApi;
    }

    protected MetricFetcher createTask(Map server) {
        return new MetricFetcher.Builder(useBulkApi(server))
                .server(server)
                .configuration(configuration)
                .soapMessageUtil(soapMessageUtil)
                .metricWriter(this)
                .build();
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
        if (metricValue != null) {
            metricWriter.printMetric(metricPath, metricValue, aggregation, timeRollup, cluster);
        }
    }

//    public static void main(String[] args) {
//
//        final Map<String, String> taskArgs = new HashMap();
//        taskArgs.put("config-file", "src/main/resources/conf/config.yml");
//        taskArgs.put("metric-file", "src/main/resources/conf/metrics.xml");
//        try {
//            final DataPowerMonitor monitor = new DataPowerMonitor();
//            monitor.execute(taskArgs, null);
//        } catch (Exception e) {
//            logger.error("Error while running the task", e);
//        }
//    }

}
