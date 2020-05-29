/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower.config;

import javax.xml.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 11:31 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Stat {
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String label;
    @XmlAttribute(name = "system-wide")
    private String systemWide;
    @XmlAttribute(name = "use-domain")
    private String useDomain;
    @XmlElement(name = "metric")
    private MetricConfig[] metricConfigs;
    @XmlElement(name = "filter")
    private String[] filters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public MetricConfig[] getMetricConfigs() {
        return metricConfigs;
    }

    public void setMetricConfigs(MetricConfig[] metricConfigs) {
        this.metricConfigs = metricConfigs;
    }

    public String getSystemWide() {
        return systemWide;
    }

    public void setSystemWide(String systemWide) {
        this.systemWide = systemWide;
    }

    public String[] getFilters() {
        return filters;
    }

    public void setFilters(String[] filters) {
        this.filters = filters;
    }

    public String getUseDomain() {
        return useDomain;
    }

    public void setUseDomain(String useDomain) {
        this.useDomain = useDomain;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Stats{
        @XmlElement(name = "stat")
        private Stat[] stats;

        public Stat[] getStats() {
            return stats;
        }

        public void setStats(Stat[] stats) {
            this.stats = stats;
        }
    }
}
