package com.devicemind.agent.controller;

import com.devicemind.agent.business.intf.IAnalysisBusiness;
import com.devicemind.agent.model.AlertAnalysisRequest;
import com.devicemind.agent.model.AlertAnalysisResponse;
import com.devicemind.agent.model.ChatRequest;
import com.devicemind.agent.model.ChatResponse;
import com.devicemind.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/analysis")
@Tag(name = "AI Agent 服务", description = "告警智能分析 & 通用问答")
public class AnalysisController {

    @Autowired
    private IAnalysisBusiness business;

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "device-mind-agent", "version", "1.0.0");
    }

    @PostMapping("/alert")
    @Operation(summary = "告警根因分析", description = "提交告警上下文，AI 返回根因分析、处理建议和严重程度评估")
    public Result<AlertAnalysisResponse> analyzeAlert(@Valid @RequestBody AlertAnalysisRequest request) {
        log.info("告警分析请求: deviceId={}, rule={}", request.getDeviceId(), request.getRuleName());
        AlertAnalysisResponse resp = business.analyze(request);
        if (resp.isSuccess()) {
            return Result.ok(resp);
        }
        return Result.fail(resp.getErrorMsg());
    }

    @PostMapping("/chat")
    @Operation(summary = "通用智能问答", description = """
            与 AI 助手对话，支持设备状态查询、数据趋势分析、告警概览、指令统计、自然语言查数据等。
            只读操作 AI 自动调用工具获取实时数据回答；
            写操作（如下发指令）返回待确认信息，需前端二次确认后才执行。""")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat 请求: deviceId={}, question={}", request.getDeviceId(), request.getQuestion());
        ChatResponse resp = business.chat(request);
        if (resp.isSuccess()) {
            return Result.ok(resp);
        }
        return Result.fail(resp.getErrorMsg());
    }
}
