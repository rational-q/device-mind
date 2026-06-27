package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SceneCreateDTO {

    @NotBlank(message = "场景名称不能为空")
    @Schema(description = "场景名称", example = "高温自动排风")
    private String name;

    @Schema(description = "场景描述", example = "3号车间温度超过40℃时自动打开排风扇")
    private String description;

    @NotNull(message = "产品ID不能为空")
    @Schema(description = "关联产品ID", example = "1")
    private Long productId;

    @NotBlank(message = "触发条件不能为空")
    @Schema(description = "触发条件JSON", example = "[{\"attr\":\"temperature\",\"operator\":\">\",\"value\":40,\"duration\":30}]")
    private String conditions;

    @NotBlank(message = "执行动作不能为空")
    @Schema(description = "执行动作JSON", example = "[{\"type\":\"COMMAND\",\"targetDeviceId\":\"FAN-01\",\"command\":\"turnOn\",\"params\":{}},{\"type\":\"DELAY\",\"seconds\":10},{\"type\":\"SMS\",\"phoneNumbers\":[\"13800138000\"],\"content\":\"3号车间温度异常\"}]")
    private String actions;
}
