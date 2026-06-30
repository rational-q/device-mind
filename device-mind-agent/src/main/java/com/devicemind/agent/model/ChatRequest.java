package com.devicemind.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "通用问答请求")
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    @Schema(description = "用户问题", example = "现在有多少设备在线？")
    private String question;

    @Schema(description = "设备ID（可选，缩小上下文范围）", example = "temp-001")
    private String deviceId;
}
