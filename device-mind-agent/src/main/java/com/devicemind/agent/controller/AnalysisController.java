package com.devicemind.agent.controller;

import com.devicemind.agent.dto.AlertAnalysisRequest;
import com.devicemind.agent.dto.AlertAnalysisResponse;
import com.devicemind.agent.dto.Nl2SqlRequest;
import com.devicemind.agent.dto.Nl2SqlResponse;
import com.devicemind.agent.service.AlertAnalysisService;
import com.devicemind.agent.service.Nl2SqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "AI Agent 服务", description = "告警智能分析 & NL2SQL 自然语言查询")
public class AnalysisController {

    private final AlertAnalysisService alertAnalysisService;
    private final Nl2SqlService nl2SqlService;

    // ==================== 健康检查 ====================

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "device-mind-agent",
                "version", "1.0.0"
        ));
    }

    // ==================== 告警分析 ====================

    @PostMapping("/analysis/alert")
    @Operation(summary = "告警根因分析", description = "提交告警上下文，AI 返回根因分析、处理建议和严重程度评估")
    public ResponseEntity<AlertAnalysisResponse> analyzeAlert(
            @Valid @RequestBody AlertAnalysisRequest request) {
        log.info("告警分析请求: deviceId={}, rule={}", request.getDeviceId(), request.getRuleName());
        AlertAnalysisResponse response = alertAnalysisService.analyze(request);
        return ResponseEntity.ok(response);
    }

    // ==================== NL2SQL ====================

    @PostMapping("/analysis/nl2sql")
    @Operation(summary = "自然语言转 SQL", description = "将自然语言查询转换为 TimescaleDB SQL 语句（不执行）")
    public ResponseEntity<Nl2SqlResponse> nl2sql(
            @Valid @RequestBody Nl2SqlRequest request) {
        log.info("NL2SQL 查询: {}", request.getQuestion());
        Nl2SqlResponse response = nl2SqlService.generateSql(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analysis/query")
    @Operation(summary = "自然语言查数据", description = "将自然语言查询转为 SQL 并执行，返回真实数据结果")
    public ResponseEntity<Nl2SqlResponse> query(
            @Valid @RequestBody Nl2SqlRequest request) {
        log.info("NL2SQL 查询+执行: {}", request.getQuestion());
        request.setExecute(true);
        Nl2SqlResponse response = nl2SqlService.generateSql(request);
        return ResponseEntity.ok(response);
    }
}
