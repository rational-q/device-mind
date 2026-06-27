package com.devicemind.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 设备数据上报请求 DTO
 * <p>
 * 请求示例：
 * <pre>
 * POST /api/device-data
 * {
 *   "deviceId": "A-102",
 *   "timestamp": 1718200000,
 *   "attrs": {
 *     "temperature": 30.5,
 *     "humidity": 65.0
 *   }
 * }
 * </pre>
 */
@Data
public class DeviceDataRequest {

    /** 设备唯一标识 */
    @NotBlank(message = "deviceId 不能为空")
    private String deviceId;

    /** 数据时间戳（epoch 秒），不传则使用服务端当前时间 */
    private Long timestamp;

    /** 属性键值对，key 为属性名，value 支持数值/字符串/枚举等多种类型 */
    @NotNull(message = "attrs 不能为空")
    private Map<String, Object> attrs;
}
