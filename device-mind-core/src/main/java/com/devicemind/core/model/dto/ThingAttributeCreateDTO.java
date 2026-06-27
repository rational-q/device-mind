package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ThingAttributeCreateDTO {

    @NotBlank
    @Schema(description = "属性标识", example = "temperature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;

    @NotBlank
    @Schema(description = "属性名称", example = "温度", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank
    @Schema(description = "数据类型", example = "DOUBLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataType;

    @Schema(description = "单位", example = "℃")
    private String unit;

    @NotBlank
    @Schema(description = "访问模式", example = "R", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessMode;

    @Schema(description = "描述")
    private String description;
}
