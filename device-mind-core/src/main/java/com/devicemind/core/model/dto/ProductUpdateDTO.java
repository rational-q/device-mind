package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProductUpdateDTO {

    @Schema(description = "产品名称")
    private String name;

    @Schema(description = "产品描述")
    private String description;

    @Schema(description = "协议类型", example = "MQTT")
    private String protocolType;

    @Schema(description = "数据格式", example = "JSON")
    private String dataFormat;

    @Schema(description = "状态", example = "ACTIVE")
    private String status;
}
