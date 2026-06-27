package com.devicemind.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import org.springframework.boot.web.client.RestTemplateBuilder;

/**
 * HTTP 客户端配置
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
