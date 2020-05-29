/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.util.StringUtils;
import com.appdynamics.monitors.util.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by abey.tom on 5/12/15.
 */
public class DataPowerMonitorTask extends MetricFetcher {
    public static final Logger logger = LoggerFactory.getLogger(DataPowerMonitorTask.class);

    @Override
    protected void fetchMetrics(List<String> selectedDomains, String serverPrefix) {
        for (String domain : selectedDomains) {
            String domainPrefix = serverPrefix + domain;
            for (Stat stat : getStats()) {
                if (!"true".equals(stat.getSystemWide())) {
                    fetchMetrics(domain, domainPrefix, stat);
                }
            }
        }
        String domain = selectedDomains.get(0);
        String prefix = StringUtils.trim(serverPrefix, "|");
        for (Stat stat : getStats()) {
            if ("true".equals(stat.getSystemWide())) {
                if(StringUtils.hasText(stat.getUseDomain())){
                    fetchMetrics(stat.getUseDomain(),prefix,stat);
                } else{
                    fetchMetrics(domain, prefix, stat);
                }
            }
        }
    }

    private void fetchMetrics(String domain, String prefix, Stat stat) {
        String statLabel = getStatLabel(prefix, stat);
        String operation = stat.getName();
        Metric[] metrics = stat.getMetrics();
        if (metrics != null && metrics.length > 0) {
            Xml[] response = getResponse(operation, domain);
            response = filter(stat.getFilters(),response);
            if (response != null) {
                extractData(statLabel, metrics, response, stat);
            }
        }
    }

}
