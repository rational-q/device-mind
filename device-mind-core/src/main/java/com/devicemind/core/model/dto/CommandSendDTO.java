package com.devicemind.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "指令下发请求")
public class CommandSendDTO {

    @NotBlank(message = "设备ID不能为空")
    @Schema(description = "目标设备ID", example = "temp-001")
    private String deviceId;

    @NotBlank(message = "指令类型不能为空")
    @Schema(description = "指令类型", example = "set_threshold")
    private String command;

    @Schema(description = "指令参数", example = "{\"temperature\": 30}")
    private Map<String, Object> params;

    @Schema(description = "幂等键（不传则自动生成）")
    private String idempotencyKey;
}
