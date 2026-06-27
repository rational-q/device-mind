package com.devicemind.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Core 服务地址配置
 * <p>
 * 对应 application.yml 中的 core-service.url
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "core-service")
public class CoreServiceConfig {

    /** Core 服务基础 URL */
    private String url = "http://localhost:8080";
}
