package com.devicemind.common.kafka.model;

import com.devicemind.common.enums.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备状态变更事件 — Broker → Core（online / offline）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusEvent {

    /** 设备ID（MQTT clientId） */
    private String deviceId;

    /** 状态：ONLINE / OFFLINE */
    private String status;

    /** 事件时间戳（epoch 秒） */
    private long timestamp;

    public static DeviceStatusEvent online(String deviceId) {
        return new DeviceStatusEvent(deviceId, DeviceStatus.ONLINE.name(), System.currentTimeMillis() / 1000);
    }

    public static DeviceStatusEvent offline(String deviceId) {
        return new DeviceStatusEvent(deviceId, DeviceStatus.OFFLINE.name(), System.currentTimeMillis() / 1000);
    }
}
