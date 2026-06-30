package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "设备数据上报请求")
public class DeviceDataRequest {

    @NotBlank(message = "deviceId 不能为空")
    @Schema(description = "设备唯一标识", example = "A-102")
    private String deviceId;

    @Schema(description = "数据时间戳（epoch 秒），不传则使用服务端当前时间", example = "1718200000")
    private Long timestamp;

    @NotNull(message = "attrs 不能为空")
    @Schema(description = "属性键值对", example = "{\"temperature\":30.5,\"humidity\":65.0}")
    private Map<String, Object> attrs;
}
