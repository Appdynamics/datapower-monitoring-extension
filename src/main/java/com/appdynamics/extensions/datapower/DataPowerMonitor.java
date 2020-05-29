/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.datapower.config.Stat;
import com.appdynamics.extensions.datapower.util.Constants;
import com.appdynamics.extensions.datapower.util.SoapMessageUtil;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;

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
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(DataPowerMonitor.class);

    private static final String METRIC_PREFIX = Constants.METRIC_PREFIX;

    private SoapMessageUtil soapMessageUtil;

    private MonitorContextConfiguration configuration;

    public DataPowerMonitor() {
        soapMessageUtil = new SoapMessageUtil();
    }

    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    public String getMonitorName() {
        return Constants.MonitorName;
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        try {
            configuration = getContextConfiguration();
            MetricWriteHelper metricWriter = tasksExecutionServiceProvider.getMetricWriteHelper();
            Map<String, ?> config = configuration.getConfigYml();
            Stat.Stats metricConfig = (Stat.Stats) configuration.getMetricsXml();
            if (config != null && metricConfig != null && metricConfig.getStats() != null) {
                List<Map> servers = (List) config.get("servers");
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        MetricFetcher task = createTask(server, metricWriter);
                        configuration.getContext().getExecutorService().execute("Datapower-" + server.get("displayName"), task);
                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                if (metricConfig == null) {
                    logger.error("The metrics.xml is not loaded due to previous errors.The task will not run");
                }
            }
        }catch (Exception e){
            logger.error("error while running the Datapower server Task", e);
        }
    }

    protected MetricFetcher createTask(Map server, MetricWriteHelper metricWriter) {
        return new MetricFetcher.Builder(useBulkApi(server))
                .withServer(server)
                .withConfiguration(configuration)
                .withSoapMessageUtil(soapMessageUtil)
                .withMetricWriteHelper(metricWriter)
                .build();
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

//    public static void main(String[] args) {
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
