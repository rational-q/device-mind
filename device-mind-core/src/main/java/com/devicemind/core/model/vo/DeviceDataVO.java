package com.devicemind.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DeviceDataVO {

    @Schema(description = "数据时间（epoch秒）")
    private Long time;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "属性名称", example = "temperature")
    private String attrName;

    @Schema(description = "属性值")
    private Double value;
}
