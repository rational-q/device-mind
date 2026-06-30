package com.devicemind.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备状态枚举
 */
@Getter
public enum DeviceStatus {

    UNKNOWN("UNKNOWN", "未知"),
    ONLINE("ONLINE", "在线"),
    OFFLINE("OFFLINE", "离线");

    private final String code;
    private final String desc;

    DeviceStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举
     */
    public static DeviceStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (DeviceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return DeviceStatus.UNKNOWN;
    }

    /**
     * 判断给定状态名称是否为在线
     */
    public static boolean isOnline(String code) {
        return ONLINE.getCode().equals(code);
    }

    /**
     * 判断给定状态名称是否为离线
     */
    public static boolean isOffline(String code) {
        return OFFLINE.getCode().equals(code);
    }
}
