package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceCreateDTO {

    @NotBlank(message = "设备ID不能为空")
    @Schema(description = "设备唯一标识", example = "A-102", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;

    @NotNull(message = "产品ID不能为空")
    @Schema(description = "所属产品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "标签")
    private String tags;
}
