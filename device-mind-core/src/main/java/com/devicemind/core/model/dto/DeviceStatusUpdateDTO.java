package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceStatusUpdateDTO {

    @NotBlank(message = "状态不能为空")
    @Schema(description = "状态", example = "ONLINE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
