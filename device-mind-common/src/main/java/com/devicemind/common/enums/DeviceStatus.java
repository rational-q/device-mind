package com.devicemind.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备状态枚举
 */
@Getter
@AllArgsConstructor
public enum DeviceStatus {

    ONLINE(1, "在线"),
    OFFLINE(0, "离线");

    private final int code;
    private final String desc;

    /**
     * 根据 code 获取枚举
     */
    public static DeviceStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeviceStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
