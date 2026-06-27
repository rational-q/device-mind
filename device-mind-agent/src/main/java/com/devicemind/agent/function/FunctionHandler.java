package com.devicemind.agent.function;

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
}
