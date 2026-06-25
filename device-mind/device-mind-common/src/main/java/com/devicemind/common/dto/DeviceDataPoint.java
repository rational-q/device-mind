package com.devicemind.common.dto;

import java.io.Serializable;

/**
 * 设备数据点
 */
public class DeviceDataPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String attrName;
    private double value;
    private long timestamp;

    public DeviceDataPoint() {
    }

    public DeviceDataPoint(String deviceId, String attrName, double value, long timestamp) {
        this.deviceId = deviceId;
        this.attrName = attrName;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DeviceDataPoint{" +
                "deviceId='" + deviceId + '\'' +
                ", attrName='" + attrName + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
