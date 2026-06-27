package com.devicemind.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 3 / SpringDoc OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deviceMindAgentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeviceMind Agent API")
                        .description("AI 智能运维 Agent — 告警分析 & NL2SQL 自然语言查询")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DeviceMind Team")));
    }
}
