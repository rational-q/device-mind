package com.devicemind.common.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备指令事件 — Core → Broker（下发指令给设备）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommandEvent {

    /** 目标设备ID */
    private String deviceId;

    /** 指令标识 */
    private String command;

    /** 指令参数 JSON */
    private String params;

    /** 幂等键 */
    private String idempotencyKey;

    /** 时间戳（epoch 秒） */
    private long timestamp;
}
