package com.appdynamics.monitors.datapower;

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
    @XmlElement(name = "metric")
    private Metric[] metrics;

    @XmlAttribute(name = "metric-type")
    private MetricType metricType;

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

    public Metric[] getMetrics() {
        return metrics;
    }

    public void setMetrics(Metric[] metrics) {
        this.metrics = metrics;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
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
