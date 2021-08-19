/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower;

import com.appdynamics.extensions.datapower.config.MetricConfig;
import com.appdynamics.extensions.datapower.config.Stat;
import com.appdynamics.extensions.datapower.util.Xml;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.StringUtils;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import java.util.List;

/**
 * Created by abey.tom on 5/12/15.
 */
public class DataPowerMonitorTask extends MetricFetcher {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(DataPowerMonitorTask.class);

    @Override
    protected List<Metric> fetchMetrics(List<String> selectedDomains, String serverPrefix) {
        List<Metric> metricsList = Lists.newArrayList();
        for (String domain : selectedDomains) {
            String domainPrefix = serverPrefix + domain;
            for (Stat stat : getStats()) {
                if (!"true".equals(stat.getSystemWide())) {
                    fetchMetrics(domain, domainPrefix, stat, metricsList);
                }
            }
        }
        String domain = selectedDomains.get(0);
        String prefix = StringUtils.trim(serverPrefix, "|");
        for (Stat stat : getStats()) {
            if ("true".equals(stat.getSystemWide())) {
                if(StringUtils.hasText(stat.getUseDomain())){
                    fetchMetrics(stat.getUseDomain(),prefix,stat, metricsList);
                } else{
                    fetchMetrics(domain, prefix, stat, metricsList);
                }
            }
        }
        return metricsList;
    }

    private void fetchMetrics(String domain, String prefix, Stat stat, List<Metric> metricsList) {
        String statLabel = getStatLabel(prefix, stat);
        String operation = stat.getName();
        MetricConfig[] metricConfigs = stat.getMetricConfigs();
        if (metricConfigs != null && metricConfigs.length > 0) {
            Xml[] response = getResponse(operation, domain);
            response = filter(stat.getFilters(),response);
            if (response != null) {
                extractData(statLabel, metricConfigs, response, stat, metricsList);
            }
        }
    }

}
