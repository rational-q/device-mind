package com.devicemind.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 统一配置（common 模块）
 * <p>
 * 各子模块通过 {@code @Import(SwaggerConfig.class)} 导入复用。
 */
@Configuration
public class SwaggerConfig {

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
