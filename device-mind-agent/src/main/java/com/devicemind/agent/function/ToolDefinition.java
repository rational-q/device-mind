package com.devicemind.agent.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek / OpenAI 兼容的 Tool 定义
 * <p>
 * 对应 API 参数中的 tools 数组项
 */
@Data
@Builder
public class ToolDefinition {

    @Builder.Default
    private String type = "function";

    private FunctionDefinition function;

    @Data
    @Builder
    public static class FunctionDefinition {
        private String name;
        private String description;

        @Builder.Default
        private Parameters parameters = Parameters.builder().build();
    }

    @Data
    @Builder
    public static class Parameters {
        @Builder.Default
        private String type = "object";

        @Singular("property")
        private Map<String, ParameterProperty> properties;

        private List<String> required;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParameterProperty {
        private String type;

        @JsonProperty("enum")
        private List<String> enumValues;
        private String description;
    }
}
