package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ThingServiceParamCreateDTO {

    @NotBlank
    @Schema(description = "参数标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;
    @NotBlank
    @Schema(description = "参数名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    @NotBlank
    @Schema(description = "数据类型", example = "DOUBLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataType;
    @Schema(description = "是否必填")
    private Boolean required = false;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "参数描述")
    private String description;
}
