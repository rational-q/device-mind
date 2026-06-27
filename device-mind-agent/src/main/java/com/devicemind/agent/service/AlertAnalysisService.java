package com.devicemind.agent.service;

import com.devicemind.agent.client.DeepSeekClient;
import com.devicemind.agent.dto.AlertAnalysisRequest;
import com.devicemind.agent.dto.AlertAnalysisResponse;
import com.devicemind.agent.function.FunctionRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 告警智能分析服务
 * <p>
 * 支持 Function Calling，DeepSeek 在分析过程中可主动调用工具查询设备详情、
 * 时序数据、告警历史等上下文，产出更准确的根因分析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertAnalysisService {

    private final DeepSeekClient deepSeekClient;
    private final FunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    /** 最大函数调用轮次，防止无限循环 */
    private static final int MAX_TOOL_ROUNDS = 5;

    /** 系统提示词 — 定义 AI 角色和分析框架 */
    private static final String SYSTEM_PROMPT = """
            你是一个物联网运维专家，负责分析设备告警的根因并提供处理建议。

            你可以使用以下工具获取更多上下文信息：
            - deviceInfo: 查询设备基本信息（名称、产品、状态、位置）
            - deviceData: 查询设备最近N小时的时序数据（可按属性过滤）
            - deviceShadow: 查询设备影子状态（最新上报值）
            - alertHistory: 查询设备最近N小时的告警历史
            - alertRules: 查询告警规则配置（阈值、运算符）

            在生成最终分析前，根据需要调用工具获取数据支撑。
            如果已有足够信息，可以直接生成分析。

            ## 输出格式
            必须返回 JSON 格式（不要加 markdown 代码块标记），字段如下：
            {
              "summary": "根因分析摘要（一句话概括）",
              "possibleCauses": ["原因1", "原因2"],
              "recommendations": ["建议1", "建议2"],
              "severity": "高/中/低"
            }

            请确保 JSON 合法可解析，不要添加额外内容。
            """;

    /**
     * 分析一条告警（Function Calling 模式）
     */
    public AlertAnalysisResponse analyze(AlertAnalysisRequest request) {
        // 1. 构建初始消息
        List<DeepSeekClient.Message> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.Message("system", SYSTEM_PROMPT));
        messages.add(new DeepSeekClient.Message("user", buildUserPrompt(request)));

        // 2. 获取工具定义
        List<com.devicemind.agent.function.ToolDefinition> tools = functionRegistry.getToolDefinitions();

        // 3. 函数调用循环（最多 5 轮）
        String rawResponse = null;
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            DeepSeekClient.ChatResponse response = deepSeekClient.chatWithTools(messages, tools);
            if (response == null) {
                return AlertAnalysisResponse.builder()
                        .success(false)
                        .errorMsg("AI 分析服务暂时不可用（API Key 未配置或网络异常）")
                        .build();
            }

            DeepSeekClient.Message assistantMsg = response.getChoices().get(0).getMessage();

            // 检查是否有函数调用
            if (assistantMsg.getToolCalls() != null && !assistantMsg.getToolCalls().isEmpty()) {
                log.info("DeepSeek 请求调用工具: round={}, tools={}",
                        round + 1, assistantMsg.getToolCalls().stream()
                                .map(tc -> tc.getFunction().getName()).toList());

                // 将 assistant 消息加入历史
                messages.add(assistantMsg);

                // 执行每个工具调用
                for (DeepSeekClient.ToolCall toolCall : assistantMsg.getToolCalls()) {
                    String name = toolCall.getFunction().getName();
                    String args = toolCall.getFunction().getArguments();
                    String result = functionRegistry.execute(name, args);
                    log.debug("工具执行完成: name={}, resultLen={}", name, result.length());
                    messages.add(new DeepSeekClient.Message(toolCall.getId(), name, result));
                }
                // 继续下一轮
                continue;
            }

            // 没有函数调用 → 这是最终回复
            rawResponse = assistantMsg.getContent();
            break;
        }

        if (rawResponse == null) {
            return AlertAnalysisResponse.builder()
                    .success(false)
                    .errorMsg("AI 分析超过最大轮次限制，请稍后重试")
                    .build();
        }

        // 4. 解析 JSON（与原有逻辑一致）
        return parseResponse(rawResponse);
    }

    /** 解析 AI 回复为结构化结果 */
    private AlertAnalysisResponse parseResponse(String rawResponse) {
        try {
            String json = rawResponse.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf('\n') + 1);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.lastIndexOf("```"));
                }
                json = json.trim();
            }

            Map<String, Object> result = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<String> causes = result.get("possibleCauses") instanceof List
                    ? (List<String>) result.get("possibleCauses")
                    : Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<String> recommendations = result.get("recommendations") instanceof List
                    ? (List<String>) result.get("recommendations")
                    : Collections.emptyList();

            return AlertAnalysisResponse.builder()
                    .success(true)
                    .summary(getStringSafe(result, "summary"))
                    .possibleCauses(causes)
                    .recommendations(recommendations)
                    .severity(getStringSafe(result, "severity"))
                    .rawResponse(rawResponse)
                    .build();

        } catch (Exception e) {
            log.warn("解析 AI 分析结果失败: {}", e.getMessage());
            return AlertAnalysisResponse.builder()
                    .success(true)
                    .summary(rawResponse.length() > 200 ? rawResponse.substring(0, 200) + "…" : rawResponse)
                    .possibleCauses(Collections.emptyList())
                    .recommendations(Collections.emptyList())
                    .severity("未知")
                    .rawResponse(rawResponse)
                    .build();
        }
    }

    /** 构建用户消息 */
    private String buildUserPrompt(AlertAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下设备告警：\n\n");
        sb.append("【设备信息】\n");
        sb.append("- 设备ID: ").append(request.getDeviceId()).append("\n");
        if (request.getProductName() != null) {
            sb.append("- 产品: ").append(request.getProductName());
        }
        if (request.getProductKey() != null) {
            sb.append(" (").append(request.getProductKey()).append(")");
        }
        sb.append("\n");
        if (request.getDeviceName() != null) {
            sb.append("- 名称: ").append(request.getDeviceName()).append("\n");
        }
        if (request.getLocation() != null) {
            sb.append("- 位置: ").append(request.getLocation()).append("\n");
        }

        sb.append("\n【告警信息】\n");
        sb.append("- 规则: ").append(request.getRuleName()).append("\n");
        sb.append("- 等级: ").append(request.getLevel()).append("\n");
        sb.append("- 监控属性: ").append(request.getMetric()).append("\n");
        sb.append("- 当前值: ").append(request.getCurrentValue()).append("\n");
        sb.append("- 阈值: ").append(request.getThreshold()).append("\n");

        if (request.getRecentData() != null && !request.getRecentData().isEmpty()) {
            sb.append("\n【近期数据趋势】\n");
            for (Map.Entry<String, List<Double>> entry : request.getRecentData().entrySet()) {
                sb.append("- ").append(entry.getKey()).append(": ");
                sb.append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    private String getStringSafe(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
