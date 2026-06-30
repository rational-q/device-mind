package com.devicemind.broker;

import com.devicemind.common.config.JacksonConfig;
import com.devicemind.common.config.KafkaErrorHandlerConfig;
import com.devicemind.common.config.KafkaProducerConfig;
import com.devicemind.common.config.KafkaTopicConfig;
import com.devicemind.common.config.RedissonConfig;
import com.devicemind.common.config.TaskExecutorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Import({
        JacksonConfig.class,
        KafkaProducerConfig.class,
        KafkaTopicConfig.class,
        KafkaErrorHandlerConfig.class,
        RedissonConfig.class,
        TaskExecutorConfig.class
})
public class BrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerApplication.class, args);
    }
}
