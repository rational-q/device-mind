package com.devicemind.core;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.devicemind.common.config.KafkaErrorHandlerConfig;
import com.devicemind.common.config.KafkaProducerConfig;
import com.devicemind.common.config.KafkaTopicConfig;
import com.devicemind.common.config.RedissonConfig;
import com.devicemind.common.config.SwaggerConfig;
import com.devicemind.common.config.TaskExecutorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DeviceMind Core 启动类
 * <p>
 * 排除 DataSource 和 MyBatis-Plus 自动配置，因为我们有多个数据源（MySQL + TimescaleDB），
 * 需要手动管理 DataSource 和 SqlSessionFactory（见 {@link com.devicemind.core.config.DataSourceConfig}）
 */
@EnableScheduling
@Import({
        SwaggerConfig.class,
        KafkaProducerConfig.class,
        KafkaTopicConfig.class,
        KafkaErrorHandlerConfig.class,
        RedissonConfig.class,
        TaskExecutorConfig.class
})
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class
})
public class CoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}
