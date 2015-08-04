package com.appdynamics.monitors.datapower;

import com.appdynamics.extensions.xml.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by abey.tom on 5/12/15.
 */
public class DataPowerMonitorTask extends MetricFetcher {
    public static final Logger logger = LoggerFactory.getLogger(DataPowerMonitorTask.class);

    @Override
    protected void fetchMetrics(List<String> selectedDomains) {
        for (String domain : selectedDomains) {
            String domainPrefix = metricPrefix + domain;
            for (Stat stat : metricConf) {
                String statLabel = getStatLabel(domainPrefix, stat);
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

}
