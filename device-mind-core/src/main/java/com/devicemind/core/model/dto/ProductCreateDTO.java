package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductCreateDTO {

    @NotBlank(message = "产品标识不能为空")
    @Schema(description = "产品标识", example = "TEMP_SENSOR_V1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productKey;

    @NotBlank(message = "产品名称不能为空")
    @Schema(description = "产品名称", example = "温湿度传感器", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "产品描述")
    private String description;

    @NotBlank(message = "协议类型不能为空")
    @Schema(description = "协议类型", example = "MQTT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String protocolType;

    @NotBlank(message = "数据格式不能为空")
    @Schema(description = "数据格式", example = "JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataFormat;
}
