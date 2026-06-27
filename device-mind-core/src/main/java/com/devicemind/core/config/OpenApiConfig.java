package com.devicemind.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 3 / SpringDoc OpenAPI 统一配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deviceMindOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeviceMind API")
                        .description("DeviceMind IoT 设备管理平台接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DeviceMind Team")));
    }
}
