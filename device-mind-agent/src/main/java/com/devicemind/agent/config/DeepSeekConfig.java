package com.devicemind.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek API 配置
 * <p>
 * 对应 application.yml 中的 deepseek.api.*
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekConfig {

    /** API 请求地址 */
    private String url = "https://api.deepseek.com";

    /** API Key */
    private String key = "";

    /** 请求超时（秒） */
    private int timeout = 30;

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 最大重试次数 */
    private int maxRetries = 2;

    /** 重试间隔（毫秒） */
    private long retryIntervalMs = 1000;
}
