package com.devicemind.common.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备数据点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDataPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String attrName;
    private Object value;
    private long timestamp;
}
