package com.devicemind.agent.business.impl;

import com.devicemind.agent.business.intf.IAnalysisBusiness;
import com.devicemind.agent.client.DeepSeekClient;
import com.devicemind.agent.function.FunctionRegistry;
import com.devicemind.agent.model.AlertAnalysisRequest;
import com.devicemind.agent.model.AlertAnalysisResponse;
import com.devicemind.agent.model.ChatRequest;
import com.devicemind.agent.model.ChatResponse;
import com.devicemind.agent.service.ConversationStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AnalysisBusiness implements IAnalysisBusiness {

    @Autowired
    private DeepSeekClient deepSeekClient;
    @Autowired
    private FunctionRegistry functionRegistry;
    @Autowired
    private ConversationStore conversationStore;

    private static final int MAX_TOOL_ROUNDS = 5;

    private static final String ALERT_SYSTEM_PROMPT = """
            你是一个物联网运维专家，负责分析设备告警的根因并提供处理建议。你可以使用工具获取设备信息、时序数据、告警历史等。
            必须返回 JSON: {"summary":"...","possibleCauses":["..."],"recommendations":["..."],"severity":"高/中/低"}
            不要加 markdown 代码块标记。""";

    @Override
    public AlertAnalysisResponse analyze(AlertAnalysisRequest request) {
        List<DeepSeekClient.Message> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.Message("system", ALERT_SYSTEM_PROMPT));
        messages.add(new DeepSeekClient.Message("user", buildAlertUserPrompt(request)));

        String rawResponse = runToolLoop(messages, functionRegistry.getToolDefinitions());
        if (rawResponse == null) {
            return AlertAnalysisResponse.builder().success(false).errorMsg("AI 分析服务暂时不可用").build();
        }
        return parseAlertResponse(rawResponse);
    }

    private static final String CHAT_SYSTEM_PROMPT = """
            你是 DeviceMind 物联网平台的智能助手，负责回答用户关于设备、数据、告警、指令等问题。

            你可以使用的只读工具：
            - deviceInfo/deviceData/deviceShadow/alertHistory/alertRules
            - deviceStatus/alertSummary/commandStats/nl2sql/projectDocs
            写操作工具（需用户确认，不可自动执行）：
            - sendCommand: 向设备下发指令，调用后返回 pending_confirmation，不会即时执行

            规则：优先使用工具获取实时数据，用中文简洁回答。超出平台范围的问题拒绝回答。""";

    @Override
    public ChatResponse chat(ChatRequest request) {
        String question = request.getQuestion();
        String deviceId = request.getDeviceId();
        String sessionId = request.getSessionId();

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = conversationStore.create();
        }
        log.info("Chat: sessionId={}, deviceId={}, question={}", sessionId, deviceId, question);

        StringBuilder userPrompt = new StringBuilder(question);
        if (deviceId != null && !deviceId.isBlank()) {
            userPrompt.insert(0, String.format("【当前上下文：设备 %s】\n", deviceId));
        }

        List<DeepSeekClient.Message> messages = conversationStore.getMessages(sessionId, CHAT_SYSTEM_PROMPT, userPrompt.toString());
        List<com.devicemind.agent.function.ToolDefinition> tools = functionRegistry.getToolDefinitions();
        List<String> toolsCalled = new ArrayList<>();
        Map<String, Object> pendingAction = new LinkedHashMap<>();

        String rawResponse = runToolLoopWithTracking(messages, tools, toolsCalled, pendingAction);

        if (rawResponse == null) {
            if (!pendingAction.isEmpty()) {
                return ChatResponse.builder().sessionId(sessionId).success(true)
                        .answer("指令已准备好，请在弹窗中确认后执行。").toolsCalled(toolsCalled).pendingAction(pendingAction).build();
            }
            return ChatResponse.builder().sessionId(sessionId).success(false).errorMsg("AI 服务暂时不可用").toolsCalled(toolsCalled).build();
        }

        conversationStore.saveRound(sessionId, userPrompt.toString(), rawResponse);

        return ChatResponse.builder().sessionId(sessionId).success(true).answer(rawResponse)
                .toolsCalled(toolsCalled).pendingAction(pendingAction.isEmpty() ? null : pendingAction).rawResponse(rawResponse).build();
    }

    // ============= shared tool loops =============

    private String runToolLoop(List<DeepSeekClient.Message> messages, List<com.devicemind.agent.function.ToolDefinition> tools) {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            DeepSeekClient.ChatResponse response = deepSeekClient.chatWithTools(messages, tools);
            if (response == null) return null;
            DeepSeekClient.Message msg = response.getChoices().get(0).getMessage();
            if (msg.getToolCalls() == null || msg.getToolCalls().isEmpty()) return msg.getContent();
            messages.add(msg);
            for (DeepSeekClient.ToolCall tc : msg.getToolCalls()) {
                messages.add(new DeepSeekClient.Message(tc.getId(), tc.getFunction().getName(),
                        functionRegistry.execute(tc.getFunction().getName(), tc.getFunction().getArguments())));
            }
        }
        return null;
    }

    private String runToolLoopWithTracking(List<DeepSeekClient.Message> messages,
                                            List<com.devicemind.agent.function.ToolDefinition> tools,
                                            List<String> toolsCalled, Map<String, Object> pendingActionOut) {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            DeepSeekClient.ChatResponse response = deepSeekClient.chatWithTools(messages, tools);
            if (response == null) return null;
            DeepSeekClient.Message msg = response.getChoices().get(0).getMessage();
            if (msg.getToolCalls() == null || msg.getToolCalls().isEmpty()) return msg.getContent();
            messages.add(msg);
            for (DeepSeekClient.ToolCall tc : msg.getToolCalls()) {
                String name = tc.getFunction().getName();
                String result = functionRegistry.execute(name, tc.getFunction().getArguments());
                toolsCalled.add(name);
                if ("sendCommand".equals(name) && result.contains("pending_confirmation")) {
                    try { pendingActionOut.putAll(JsonUtil.fromJson(result, new TypeReference<Map<String, Object>>() {})); } catch (Exception e) { log.warn("解析 pendingAction 失败", e); }
                }
                messages.add(new DeepSeekClient.Message(tc.getId(), name, result));
            }
        }
        return null;
    }

    private String buildAlertUserPrompt(AlertAnalysisRequest r) {
        StringBuilder sb = new StringBuilder("请分析以下设备告警：\n\n【设备信息】\n- 设备ID: ").append(r.getDeviceId()).append("\n");
        if (r.getProductName() != null) sb.append("- 产品: ").append(r.getProductName());
        if (r.getProductKey() != null) sb.append(" (").append(r.getProductKey()).append(")");
        sb.append("\n");
        sb.append("\n【告警信息】\n- 规则: ").append(r.getRuleName()).append("\n- 等级: ").append(r.getLevel())
          .append("\n- 属性: ").append(r.getMetric()).append("\n- 当前值: ").append(r.getCurrentValue())
          .append("\n- 阈值: ").append(r.getThreshold()).append("\n");
        return sb.toString();
    }

    private AlertAnalysisResponse parseAlertResponse(String raw) {
        try {
            String json = raw.trim();
            if (json.startsWith("```")) { json = json.substring(json.indexOf('\n') + 1); if (json.endsWith("```")) json = json.substring(0, json.lastIndexOf("```")); json = json.trim(); }
            Map<String, Object> r = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked") List<String> causes = r.get("possibleCauses") instanceof List ? (List<String>) r.get("possibleCauses") : Collections.emptyList();
            @SuppressWarnings("unchecked") List<String> recs = r.get("recommendations") instanceof List ? (List<String>) r.get("recommendations") : Collections.emptyList();
            return AlertAnalysisResponse.builder().success(true).summary(str(r, "summary")).possibleCauses(causes).recommendations(recs).severity(str(r, "severity")).rawResponse(raw).build();
        } catch (Exception e) {
            return AlertAnalysisResponse.builder().success(true).summary(raw.length() > 200 ? raw.substring(0, 200) + "…" : raw)
                    .possibleCauses(Collections.emptyList()).recommendations(Collections.emptyList()).severity("未知").rawResponse(raw).build();
        }
    }

    private String str(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? v.toString() : ""; }
}
