package com.devicemind.agent.client;

import com.devicemind.agent.config.DeepSeekConfig;
import com.devicemind.agent.function.ToolDefinition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek API 客户端
 * <p>
 * 封装对 DeepSeek Chat API 的 HTTP 调用，支持超时、重试、降级。
 * API 文档：https://api-docs.deepseek.com/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekClient {

    private final RestTemplate restTemplate;
    private final DeepSeekConfig config;

    /**
     * 调用 DeepSeek 对话 API（非流式）
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return AI 回复内容，失败返回 null
     */
    public String chat(String systemPrompt, String userMessage) {
        if (config.getKey() == null || config.getKey().isBlank()) {
            log.warn("DeepSeek API Key 未配置，返回降级响应");
            return null;
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", userMessage));

        ChatRequest request = ChatRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .temperature(0.7)
                .maxTokens(2048)
                .build();

        // 带重试的调用
        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                log.debug("调用 DeepSeek API, attempt={}/{}", attempt + 1, config.getMaxRetries() + 1);

                // 构建请求头（Bearer Token）
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(config.getKey());
                HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

                ChatResponse response = restTemplate.exchange(
                        config.getUrl() + "/chat/completions",
                        HttpMethod.POST,
                        entity,
                        ChatResponse.class).getBody();

                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.debug("DeepSeek API 调用成功, tokens={}", response.getUsage());
                    return content;
                }
                log.warn("DeepSeek API 返回空结果, attempt={}", attempt + 1);

            } catch (Exception e) {
                log.warn("DeepSeek API 调用失败 (attempt={}/{}): {}",
                        attempt + 1, config.getMaxRetries() + 1, e.getMessage());
                if (attempt < config.getMaxRetries()) {
                    try {
                        Thread.sleep(config.getRetryIntervalMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("DeepSeek API 调用全部重试失败，返回降级响应");
        return null;
    }

    /**
     * 调用 DeepSeek 对话 API 支持 Function Calling
     *
     * @param messages 消息列表（需包含 system 消息）
     * @param tools    Function Calling 工具定义列表
     * @return ChatResponse（含 tool_calls 或 content），失败返回 null
     */
    public ChatResponse chatWithTools(List<Message> messages, List<ToolDefinition> tools) {
        if (config.getKey() == null || config.getKey().isBlank()) {
            log.warn("DeepSeek API Key 未配置");
            return null;
        }

        ChatRequest request = ChatRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .temperature(0.7)
                .maxTokens(4096)
                .tools(tools)
                .build();

        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(config.getKey());
                HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

                ChatResponse response = restTemplate.exchange(
                        config.getUrl() + "/chat/completions",
                        HttpMethod.POST,
                        entity,
                        ChatResponse.class).getBody();

                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                    log.debug("DeepSeek API 调用成功, tokens={}", response.getUsage());
                    return response;
                }
                log.warn("DeepSeek API 返回空结果, attempt={}", attempt + 1);

            } catch (Exception e) {
                log.warn("DeepSeek API 调用失败 (attempt={}/{}): {}",
                        attempt + 1, config.getMaxRetries() + 1, e.getMessage());
                if (attempt < config.getMaxRetries()) {
                    try {
                        Thread.sleep(config.getRetryIntervalMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("DeepSeek API 调用全部重试失败");
        return null;
    }

    // ==================== 内部 DTO ====================

    @Data
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<ToolDefinition> tools;

        public static ChatRequestBuilder builder() {
            return new ChatRequestBuilder();
        }

        public static class ChatRequestBuilder {
            private String model;
            private List<Message> messages;
            private double temperature;
            private int maxTokens;
            private List<ToolDefinition> tools;

            ChatRequestBuilder() {}

            public ChatRequestBuilder model(String model) { this.model = model; return this; }
            public ChatRequestBuilder messages(List<Message> messages) { this.messages = messages; return this; }
            public ChatRequestBuilder temperature(double temperature) { this.temperature = temperature; return this; }
            public ChatRequestBuilder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
            public ChatRequestBuilder tools(List<ToolDefinition> tools) { this.tools = tools; return this; }

            public ChatRequest build() {
                ChatRequest req = new ChatRequest();
                req.model = this.model;
                req.messages = this.messages;
                req.temperature = this.temperature;
                req.maxTokens = this.maxTokens;
                req.tools = this.tools;
                return req;
            }
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private String content;
        private String name;
        @JsonProperty("tool_call_id")
        private String toolCallId;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        /** 构造 tool 角色的消息 */
        public Message(String toolCallId, String name, String content) {
            this.role = "tool";
            this.toolCallId = toolCallId;
            this.name = name;
            this.content = content;
        }
    }

    @Data
    public static class ToolCall {
        private String id;
        private String type = "function";
        private FunctionCall function;
    }

    @Data
    public static class FunctionCall {
        private String name;
        private String arguments;
    }

    @Data
    public static class ChatResponse {
        private String id;
        private List<Choice> choices;
        private Usage usage;
        private long created;
    }

    @Data
    public static class Choice {
        private int index;
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;

        @Override
        public String toString() {
            return String.format("prompt=%d, completion=%d, total=%d",
                    promptTokens, completionTokens, totalTokens);
        }
    }
}
