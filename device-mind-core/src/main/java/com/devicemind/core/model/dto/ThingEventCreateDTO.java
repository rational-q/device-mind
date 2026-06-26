package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ThingEventCreateDTO {

    @NotBlank
    @Schema(description = "事件标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identifier;
    @NotBlank
    @Schema(description = "事件名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    @NotBlank
    @Schema(description = "事件类型", example = "ALERT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;
    @Schema(description = "事件描述")
    private String description;
}
