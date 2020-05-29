/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.datapower.config;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/30/14
 * Time: 11:32 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class MetricConfig {

    @XmlAttribute
    private String label;

    @XmlAttribute(name = "label-xpath")
    private String labelXpath;

    @XmlAttribute(name = "value-xpath")
    private String valueXpath;

    @XmlAttribute
    private BigDecimal multiplier;

    @XmlAttribute(name = "label-prefix")
    private String labelPrefix;

    @XmlAttribute(name = "label-suffix")
    private String labelSuffix;

    @XmlAttribute(name = "label-delim")
    private String labelDelim;

    @XmlAttribute
    private String delta;

    @XmlAttribute
    private String aggregationType;

    @XmlAttribute
    private String timeRollUpType;

    @XmlAttribute
    private String clusterRollUpType;

    @XmlElement(name = "convert")
    private MetricConverter[] converters;

    @XmlAttribute(name="isBoolean")
    private String isBoolean = "false";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelXpath() {
        return labelXpath;
    }

    public void setLabelXpath(String labelXpath) {
        this.labelXpath = labelXpath;
    }

    public String getValueXpath() {
        return valueXpath;
    }

    public void setValueXpath(String valueXpath) {
        this.valueXpath = valueXpath;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public String getLabelSuffix() {
        return labelSuffix;
    }

    public void setLabelSuffix(String labelSuffix) {
        this.labelSuffix = labelSuffix;
    }

    public String getLabelPrefix() {
        return labelPrefix;
    }

    public void setLabelPrefix(String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public MetricConverter[] getConverters() {
        return converters;
    }

    public void setConverters(MetricConverter[] converters) {
        this.converters = converters;
    }

    public String getLabelDelim() {
        return labelDelim;
    }

    public void setLabelDelim(String labelDelim) {
        this.labelDelim = labelDelim;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "label='" + label + '\'' +
                ", labelXpath='" + labelXpath + '\'' +
                ", valueXpath='" + valueXpath + '\'' +
                ", multiplier=" + multiplier +
                ", labelPrefix='" + labelPrefix + '\'' +
                ", labelSuffix='" + labelSuffix + '\'' +
                '}';
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public String getTimeRollUpType() {
        return timeRollUpType;
    }

    public void setTimeRollUpType(String timeRollUpType) {
        this.timeRollUpType = timeRollUpType;
    }

    public String getClusterRollUpType() {
        return clusterRollUpType;
    }

    public void setClusterRollUpType(String clusterRollUpType) {
        this.clusterRollUpType = clusterRollUpType;
    }

    public String getIsBoolean() {
        return isBoolean;
    }

    public void setIsBoolean(String isBoolean) {
        this.isBoolean = isBoolean;
    }
}
