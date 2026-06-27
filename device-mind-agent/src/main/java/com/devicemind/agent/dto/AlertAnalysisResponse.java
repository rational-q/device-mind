package com.devicemind.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "告警分析结果")
public class AlertAnalysisResponse {

    @Schema(description = "分析是否成功")
    private boolean success;

    @Schema(description = "错误信息（失败时）")
    private String errorMsg;

    @Schema(description = "根因分析摘要", example = "传感器附近散热风扇故障导致温度持续上升")
    private String summary;

    @Schema(description = "可能原因列表")
    private List<String> possibleCauses;

    @Schema(description = "处理建议")
    private List<String> recommendations;

    @Schema(description = "严重程度评估", example = "中")
    private String severity;

    @Schema(description = "原始AI回复")
    private String rawResponse;
}
