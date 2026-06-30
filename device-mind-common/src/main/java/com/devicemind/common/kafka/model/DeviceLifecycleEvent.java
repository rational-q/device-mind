package com.devicemind.common.kafka.model;

import com.devicemind.common.enums.LifecycleAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备生命周期事件 — Core → Broker（REGISTER / UNREGISTER）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLifecycleEvent {

    /** 操作类型 */
    private LifecycleAction action;

    /** 设备ID（MQTT clientId） */
    private String deviceId;

    /** 事件时间戳（epoch 秒） */
    private long timestamp;

    public static DeviceLifecycleEvent register(String deviceId) {
        return new DeviceLifecycleEvent(LifecycleAction.REGISTER, deviceId, System.currentTimeMillis() / 1000);
    }

    public static DeviceLifecycleEvent unregister(String deviceId) {
        return new DeviceLifecycleEvent(LifecycleAction.UNREGISTER, deviceId, System.currentTimeMillis() / 1000);
    }
}
