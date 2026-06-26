package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ThingServiceCreateDTO {

    @NotBlank
    @Schema(description = "服务标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;
    @NotBlank
    @Schema(description = "服务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    @NotBlank
    @Schema(description = "调用类型", example = "ASYNC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String callType;
    @Schema(description = "服务描述")
    private String description;
    @NotNull
    @Schema(description = "参数列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ThingServiceParamCreateDTO> params;
}
