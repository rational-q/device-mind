package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DeviceUpdateDTO {

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "标签")
    private String tags;
}
