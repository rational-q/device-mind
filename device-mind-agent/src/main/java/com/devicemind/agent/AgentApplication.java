package com.devicemind.agent;

import com.devicemind.common.config.JacksonConfig;
import com.devicemind.common.config.RedissonConfig;
import com.devicemind.common.config.SwaggerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({JacksonConfig.class, SwaggerConfig.class, RedissonConfig.class})
@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
