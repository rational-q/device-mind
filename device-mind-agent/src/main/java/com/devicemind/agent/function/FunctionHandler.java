package com.devicemind.agent.function;

import com.devicemind.common.utils.JsonUtil;

import java.util.Map;

/**
 * 函数处理器接口
 * <p>
 * 每个函数对应一个 Handler，Spring 自动收集到 {@link FunctionRegistry}
 */
public interface FunctionHandler {

    /** 函数名称（DeepSeek tool name） */
    String getFunctionName();

    /** 函数定义（描述 + 参数 schema，传给 DeepSeek） */
    ToolDefinition getToolDefinition();

    /**
     * 执行函数
     *
     * @param argumentsJson  DeepSeek 传入的参数 JSON
     * @return 执行结果 JSON 字符串
     */
    String execute(String argumentsJson);

    /**
     * 构造统一转义的错误结果 JSON。
     * <p>
     * 用 {@link JsonUtil} 序列化而非手工拼接，避免错误消息中的引号 / 换行 /
     * 反斜杠产生非法 JSON 回灌给模型导致解析异常。
     */
    static String errorJson(String message) {
        try {
            return JsonUtil.toJson(Map.of("error", message == null ? "" : message));
        } catch (Exception e) {
            return "{\"error\":\"内部错误\"}";
        }
    }
}
