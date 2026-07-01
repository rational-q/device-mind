package com.devicemind.agent.function.handler;

import com.devicemind.agent.function.FunctionHandler;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 指令下发工具（待确认模式）
 * <p>
 * <b>安全设计</b>：此工具不会直接执行指令下发。AI 仅负责将用户的自然语言指令
 * 转换为结构化的命令参数，返回 {@code pendingAction} 标记，由前端弹窗让用户
 * 二次确认后，再调用 Core 的指令下发接口真正执行。
 * <p>
 * 流程：
 * <ol>
 *   <li>用户："把 temp-001 的温度阈值调到 30"</li>
 *   <li>AI → 调用 sendCommand 工具 → 返回 pending_confirmation</li>
 *   <li>AnalysisBusiness 检测到 pendingAction → 放入 ChatResponse.pendingAction</li>
 *   <li>前端弹确认框</li>
 *   <li>用户点确认 → 前端调 Core API 真正下发</li>
 * </ol>
 */
@Slf4j
@Component
public class
SendCommandHandler implements FunctionHandler {
    /** 可通过 set 命令修改的常见属性（白名单） */
    private static final List<String> ALLOWED_COMMANDS = List.of(
            "set_threshold", "set_interval", "set_mode", "reboot", "reset",
            "set_power", "set_speed", "set_brightness", "set_temperature",
            "calibrate", "lock", "unlock", "open", "close", "start", "stop"
    );

    @Override
    public String getFunctionName() {
        return "sendCommand";
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("sendCommand")
                        .description("""
                                向设备下发指令（需要用户确认后才会真正执行）。
                                支持的命令类型：set_threshold（设置阈值）、set_interval（设置上报间隔）、
                                set_mode（切换模式）、reboot（重启设备）、reset（恢复出厂设置）。
                                此操作有安全风险，执行前必须征得用户确认。""")
                        .parameters(ToolDefinition.Parameters.builder()
                                .property("deviceId", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("目标设备ID，例如 temp-001")
                                        .build())
                                .property("command", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("命令类型，如 set_threshold、set_interval、reboot 等")
                                        .build())
                                .property("params", ToolDefinition.ParameterProperty.builder()
                                        .type("object")
                                        .description("命令参数，JSON 对象，如 {\"temperature\": 30}")
                                        .build())
                                .property("reason", ToolDefinition.ParameterProperty.builder()
                                        .type("string")
                                        .description("下发原因（给用户确认时展示）")
                                        .build())
                                .required(List.of("deviceId", "command"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = JsonUtil.readTree(argumentsJson);
            String deviceId = args.get("deviceId").asText();
            String command = args.get("command").asText();

            // 白名单校验
            if (ALLOWED_COMMANDS.stream().noneMatch(command::equalsIgnoreCase)) {
                return String.format(
                        "{\"action\":\"rejected\",\"reason\":\"不支持的命令类型: %s，仅支持: %s\"}",
                        command, String.join(", ", ALLOWED_COMMANDS));
            }

            Map<String, Object> pendingAction = new LinkedHashMap<>();
            pendingAction.put("action", "sendCommand");
            pendingAction.put("status", "pending_confirmation");
            pendingAction.put("deviceId", deviceId);
            pendingAction.put("command", command);

            if (args.has("params") && !args.get("params").isNull()) {
                pendingAction.put("params", JsonUtil.treeToValue(args.get("params"), Map.class));
            }

            String reason = args.has("reason") && !args.get("reason").isNull()
                    ? args.get("reason").asText()
                    : String.format("向 %s 下发 %s 指令", deviceId, command);
            pendingAction.put("message", "⚠️ 该操作需要您确认：" + reason);

            log.info("sendCommand 待确认: deviceId={}, command={}", deviceId, command);
            return JsonUtil.toJson(pendingAction);
        } catch (Exception e) {
            return com.devicemind.agent.function.FunctionHandler.errorJson("参数解析失败: " + e.getMessage());
        }
    }
}
