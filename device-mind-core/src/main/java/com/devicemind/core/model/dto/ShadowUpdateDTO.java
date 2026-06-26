package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ShadowUpdateDTO {
    @NotNull(message = "期望状态不能为空")
    @Schema(description = "期望状态键值对", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> desired;
}
