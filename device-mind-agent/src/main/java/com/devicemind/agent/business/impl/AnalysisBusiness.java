package com.devicemind.agent.business.impl;

import com.devicemind.agent.business.intf.IAnalysisBusiness;
import com.devicemind.agent.client.DeepSeekClient;
import com.devicemind.agent.function.FunctionRegistry;
import com.devicemind.agent.model.AlertAnalysisRequest;
import com.devicemind.agent.model.AlertAnalysisResponse;
import com.devicemind.agent.model.ChatRequest;
import com.devicemind.agent.model.ChatResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 分析业务实现 — 收敛了 AlertAnalysisService 和 AgentService
 */
@Slf4j
@Service
public class AnalysisBusiness implements IAnalysisBusiness {

    @Autowired
    private DeepSeekClient deepSeekClient;
    @Autowired
    private FunctionRegistry functionRegistry;
    private static final int MAX_TOOL_ROUNDS = 5;

    // ==================== 告警分析 ====================

    private static final String ALERT_SYSTEM_PROMPT = """
            你是一个物联网运维专家，负责分析设备告警的根因并提供处理建议。

            你可以使用以下工具获取更多上下文信息：
            - deviceInfo: 查询设备基本信息（名称、产品、状态、位置）
            - deviceData: 查询设备最近N小时的时序数据（可按属性过滤）
            - deviceShadow: 查询设备影子状态（最新上报值）
            - alertHistory: 查询设备最近N小时的告警历史
            - alertRules: 查询告警规则配置（阈值、运算符）
            - projectDocs: 查询项目架构、技术方案、消息链路设计等知识

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

    @Override
    public AlertAnalysisResponse analyze(AlertAnalysisRequest request) {
        List<DeepSeekClient.Message> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.Message("system", ALERT_SYSTEM_PROMPT));
        messages.add(new DeepSeekClient.Message("user", buildAlertUserPrompt(request)));

        List<com.devicemind.agent.function.ToolDefinition> tools = functionRegistry.getToolDefinitions();

        String rawResponse = runToolLoop(messages, tools);
        if (rawResponse == null) {
            return AlertAnalysisResponse.builder()
                    .success(false)
                    .errorMsg("AI 分析服务暂时不可用")
                    .build();
        }
        return parseAlertResponse(rawResponse);
    }

    // ==================== 通用问答 ====================

    private static final String CHAT_SYSTEM_PROMPT = """
            你是 DeviceMind 物联网平台的智能助手，负责回答用户关于设备、数据、告警、指令等问题。

            ## 你可以使用的工具
            只读工具（自动调用，无需确认）：
            - deviceInfo: 查询设备基本信息（名称、产品、状态、位置、固件版本）
            - deviceData: 查询设备最近N小时的时序数据（温度、湿度等属性）
            - deviceShadow: 查询设备影子（最新上报值 vs 期望值）
            - alertHistory: 查询设备历史告警记录
            - alertRules: 查询告警规则配置（阈值、运算符、持续时间）
            - deviceStatus: 查询设备在线状态统计（在线数、离线数、总数）
            - alertSummary: 查询近N小时告警概览（按等级分布）
            - commandStats: 查询指令下发执行统计（成功率）
            - nl2sql: 将自然语言转为 SQL 查询 TimescaleDB 时序数据。
              当 deviceData 工具无法满足需求时使用（如聚合统计、条件筛选、多属性对比等）
            - projectDocs: 查询 DeviceMind 平台本身的项目知识，包括架构设计、技术方案、
              消息链路、Kafka 可靠性、MQTT 会话、Agent 设计、关键决策等。
              当用户询问项目本身的设计或技术问题时调用。

            写操作工具（需用户确认，不可自动执行）：
            - sendCommand: 向设备下发指令（set_threshold/set_interval/reboot/reset 等）。
              调用后返回 pending_confirmation，不会即时执行。

            ## 回答规则
            1. 优先使用工具获取实时数据，不要编造数据。
            2. 用中文回答，简洁清晰，直接给出结论。
            3. 如果用户问了平台能力范围之外的问题（如天气、新闻、娱乐），回答：
               "抱歉，我是 DeviceMind 平台专用助手，专注于设备管理、数据分析和告警处理。
               你可以问我设备状态、数据趋势、告警信息、指令执行等问题。"
            4. 如果调用 sendCommand 工具，说明操作已准备好，等待用户确认。
            5. 回答末尾不要加"如有其他问题随时问我"之类的客套话。
            """;

    @Override
    public ChatResponse chat(ChatRequest request) {
        String question = request.getQuestion();
        String deviceId = request.getDeviceId();
        log.info("Chat: deviceId={}, question={}", deviceId, question);

        List<DeepSeekClient.Message> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.Message("system", CHAT_SYSTEM_PROMPT));

        StringBuilder userPrompt = new StringBuilder(question);
        if (deviceId != null && !deviceId.isBlank()) {
            userPrompt.insert(0, String.format("【当前上下文：设备 %s】\n", deviceId));
        }
        messages.add(new DeepSeekClient.Message("user", userPrompt.toString()));

        List<com.devicemind.agent.function.ToolDefinition> tools = functionRegistry.getToolDefinitions();
        List<String> toolsCalled = new ArrayList<>();
        Map<String, Object> pendingAction = new java.util.LinkedHashMap<>();

        String rawResponse = runToolLoopWithTracking(messages, tools, toolsCalled, pendingAction);

        if (rawResponse == null) {
            // 检查是否有待确认操作
            if (pendingAction != null) {
                return ChatResponse.builder()
                        .success(true)
                        .answer("指令已准备好，请在弹窗中确认后执行。")
                        .toolsCalled(toolsCalled)
                        .pendingAction(pendingAction)
                        .build();
            }
            return ChatResponse.builder()
                    .success(false)
                    .errorMsg("AI 服务暂时不可用")
                    .toolsCalled(toolsCalled)
                    .build();
        }

        return ChatResponse.builder()
                .success(true)
                .answer(rawResponse)
                .toolsCalled(toolsCalled)
                .pendingAction(pendingAction.isEmpty() ? null : pendingAction)
                .rawResponse(rawResponse)
                .build();
    }

    // ==================== 共享的工具调用循环 ====================

    /** 工具调用循环 — 告警分析用 */
    private String runToolLoop(List<DeepSeekClient.Message> messages,
                                List<com.devicemind.agent.function.ToolDefinition> tools) {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            DeepSeekClient.ChatResponse response = deepSeekClient.chatWithTools(messages, tools);
            if (response == null) return null;

            DeepSeekClient.Message msg = response.getChoices().get(0).getMessage();
            if (msg.getToolCalls() == null || msg.getToolCalls().isEmpty()) {
                return msg.getContent();
            }

            messages.add(msg);
            for (DeepSeekClient.ToolCall tc : msg.getToolCalls()) {
                String result = functionRegistry.execute(tc.getFunction().getName(), tc.getFunction().getArguments());
                messages.add(new DeepSeekClient.Message(tc.getId(), tc.getFunction().getName(), result));
            }
        }
        return null;
    }

    /** 工具调用循环 — 通用问答用（带回执检测） */
    private String runToolLoopWithTracking(List<DeepSeekClient.Message> messages,
                                            List<com.devicemind.agent.function.ToolDefinition> tools,
                                            List<String> toolsCalled,
                                            Map<String, Object> pendingActionOut) {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            DeepSeekClient.ChatResponse response = deepSeekClient.chatWithTools(messages, tools);
            if (response == null) return null;

            DeepSeekClient.Message msg = response.getChoices().get(0).getMessage();
            if (msg.getToolCalls() == null || msg.getToolCalls().isEmpty()) {
                return msg.getContent();
            }

            messages.add(msg);
            for (DeepSeekClient.ToolCall tc : msg.getToolCalls()) {
                String name = tc.getFunction().getName();
                String args = tc.getFunction().getArguments();
                String result = functionRegistry.execute(name, args);
                toolsCalled.add(name);

                // 检测 sendCommand 的 pendingAction
                if ("sendCommand".equals(name) && result.contains("pending_confirmation")) {
                    try {
                        Map<String, Object> pa = JsonUtil.fromJson(result,
                                new TypeReference<Map<String, Object>>() {});
                        if (pendingActionOut != null) pendingActionOut.putAll(pa);
                    } catch (Exception e) {
                        log.warn("解析 pendingAction 失败", e);
                    }
                }

                messages.add(new DeepSeekClient.Message(tc.getId(), name, result));
            }
        }
        return null;
    }

    // ==================== 告警分析辅助方法 ====================

    private String buildAlertUserPrompt(AlertAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下设备告警：\n\n");
        sb.append("【设备信息】\n");
        sb.append("- 设备ID: ").append(request.getDeviceId()).append("\n");
        if (request.getProductName() != null) sb.append("- 产品: ").append(request.getProductName());
        if (request.getProductKey() != null) sb.append(" (").append(request.getProductKey()).append(")");
        sb.append("\n");
        if (request.getDeviceName() != null) sb.append("- 名称: ").append(request.getDeviceName()).append("\n");
        if (request.getLocation() != null) sb.append("- 位置: ").append(request.getLocation()).append("\n");
        sb.append("\n【告警信息】\n");
        sb.append("- 规则: ").append(request.getRuleName()).append("\n");
        sb.append("- 等级: ").append(request.getLevel()).append("\n");
        sb.append("- 监控属性: ").append(request.getMetric()).append("\n");
        sb.append("- 当前值: ").append(request.getCurrentValue()).append("\n");
        sb.append("- 阈值: ").append(request.getThreshold()).append("\n");
        if (request.getRecentData() != null && !request.getRecentData().isEmpty()) {
            sb.append("\n【近期数据趋势】\n");
            request.getRecentData().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        return sb.toString();
    }

    private AlertAnalysisResponse parseAlertResponse(String raw) {
        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf('\n') + 1);
                if (json.endsWith("```")) json = json.substring(0, json.lastIndexOf("```"));
                json = json.trim();
            }
            Map<String, Object> result = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<String> causes = result.get("possibleCauses") instanceof List
                    ? (List<String>) result.get("possibleCauses") : Collections.emptyList();
            @SuppressWarnings("unchecked")
            List<String> recs = result.get("recommendations") instanceof List
                    ? (List<String>) result.get("recommendations") : Collections.emptyList();

            return AlertAnalysisResponse.builder()
                    .success(true)
                    .summary(getStr(result, "summary"))
                    .possibleCauses(causes)
                    .recommendations(recs)
                    .severity(getStr(result, "severity"))
                    .rawResponse(raw)
                    .build();
        } catch (Exception e) {
            log.warn("解析 AI 分析结果失败: {}", e.getMessage());
            return AlertAnalysisResponse.builder()
                    .success(true)
                    .summary(raw.length() > 200 ? raw.substring(0, 200) + "…" : raw)
                    .possibleCauses(Collections.emptyList())
                    .recommendations(Collections.emptyList())
                    .severity("未知")
                    .rawResponse(raw)
                    .build();
        }
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
