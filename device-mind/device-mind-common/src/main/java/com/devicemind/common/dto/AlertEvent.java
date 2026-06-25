package com.devicemind.common.dto;

import java.io.Serializable;

/**
 * 告警事件
 */
public class AlertEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String ruleName;
    private String level;
    private double currentValue;
    private double threshold;
    private String trendData;

    public AlertEvent() {
    }

    public AlertEvent(String deviceId, String ruleName, String level, double currentValue, double threshold, String trendData) {
        this.deviceId = deviceId;
        this.ruleName = ruleName;
        this.level = level;
        this.currentValue = currentValue;
        this.threshold = threshold;
        this.trendData = trendData;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getTrendData() {
        return trendData;
    }

    public void setTrendData(String trendData) {
        this.trendData = trendData;
    }

    @Override
    public String toString() {
        return "AlertEvent{" +
                "deviceId='" + deviceId + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", level='" + level + '\'' +
                ", currentValue=" + currentValue +
                ", threshold=" + threshold +
                ", trendData='" + trendData + '\'' +
                '}';
    }
}
