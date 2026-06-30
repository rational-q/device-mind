package com.devicemind.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "通用问答响应")
public class ChatResponse {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "AI 文本回复")
    private String answer;

    @Schema(description = "本次对话调用的工具列表")
    private List<String> toolsCalled;

    @Schema(description = "待确认的操作（写操作时返回，需前端二次确认）")
    private Map<String, Object> pendingAction;

    @Schema(description = "AI 原始回复（调试用）")
    private String rawResponse;
}
