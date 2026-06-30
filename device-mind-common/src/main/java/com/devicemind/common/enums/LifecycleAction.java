package com.devicemind.common.enums;

import lombok.Getter;

/**
 * 设备生命周期操作类型
 */
@Getter
public enum LifecycleAction {

    REGISTER("REGISTER", "注册设备"),
    UNREGISTER("UNREGISTER", "卸载设备");

    private final String code;
    private final String desc;

    LifecycleAction(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举
     */
    public static LifecycleAction of(String code) {
        if (code == null) {
            return null;
        }
        for (LifecycleAction status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
