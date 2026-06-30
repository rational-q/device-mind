package com.devicemind.agent.function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.devicemind.common.utils.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 函数注册中心
 * <p>
 * 自动收集所有 {@link FunctionHandler} Bean，按名称索引。
 * 被 {@link com.devicemind.agent.business.impl.AnalysisBusiness} 使用。
 */
@Slf4j
@Component
public class FunctionRegistry {

    private final Map<String, FunctionHandler> handlers;

    @Getter
    private final List<ToolDefinition> toolDefinitions;
    public FunctionRegistry(List<FunctionHandler> handlerList) {        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(FunctionHandler::getFunctionName, h -> h));
        this.toolDefinitions = handlerList.stream()
                .map(FunctionHandler::getToolDefinition)
                .collect(Collectors.toList());
        log.info("FunctionRegistry 初始化完成，已注册 {} 个函数: {}", handlers.size(), handlers.keySet());
    }

    /**
     * 执行函数
     *
     * @param name        函数名
     * @param argsJson    参数 JSON
     * @return 执行结果 JSON
     */
    public String execute(String name, String argsJson) {
        FunctionHandler handler = handlers.get(name);
        if (handler == null) {
            log.warn("未注册的函数调用: {}", name);
            return "{\"error\":\"未知函数: " + name + "\"}";
        }
        try {
            String result = handler.execute(argsJson);
            log.debug("函数执行成功: name={}, args={}", name, argsJson);
            return result;
        } catch (Exception e) {
            log.error("函数执行失败: name={}, args={}", name, argsJson, e);
            return "{\"error\":\"函数执行异常: " + e.getMessage() + "\"}";
        }
    }
}
