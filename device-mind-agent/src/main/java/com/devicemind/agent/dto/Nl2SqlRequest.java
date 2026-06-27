package com.devicemind.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "NL2SQL 查询请求")
public class Nl2SqlRequest {

    @NotBlank(message = "自然语言查询不能为空")
    @Schema(description = "自然语言查询", example = "最近1小时温度超过30度的设备有哪些")
    private String question;

    @Schema(description = "产品类型（可选，缩小表扫描范围）", example = "TEMP_SENSOR_V1")
    private String productKey;

    @Schema(description = "设备ID（可选）", example = "A-102")
    private String deviceId;

    @Schema(description = "是否执行查询并返回结果", example = "false")
    private boolean execute = false;
}
