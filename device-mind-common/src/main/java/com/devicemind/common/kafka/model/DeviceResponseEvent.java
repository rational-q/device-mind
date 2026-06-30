package com.devicemind.common.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备指令响应事件 — Broker → Core（设备回执）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponseEvent {

    /** 设备ID */
    private String deviceId;

    /** 指令标识 */
    private String command;

    /** 幂等键 */
    private String idempotencyKey;

    /** 回执数据 JSON */
    private String data;

    /** 时间戳（epoch 秒） */
    private long timestamp;
}
