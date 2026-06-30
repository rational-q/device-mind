package com.devicemind.broker.kafka.consumer;

import com.devicemind.broker.service.DeviceAuthService;
import com.devicemind.common.exception.KafkaConsumeFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.common.kafka.model.DeviceLifecycleEvent;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * 设备生命周期消费者 — Core → Broker（register / unregister）
 * <p>
 * 异常抛出，由 CommonErrorHandler 处理重试。
 */
@Slf4j
@Service
public class DeviceLifecycleConsumer {
    @Autowired
    private DeviceAuthService deviceAuthService;

    @KafkaListener(topics = "${kafka.topics.device-lifecycle}", groupId = "${spring.kafka.consumer.group-id:broker-group}")
    public void onMessage(String message, Acknowledgment ack) {
        try {
            DeviceLifecycleEvent event = JsonUtil.fromJson(message, DeviceLifecycleEvent.class);
            log.info("收到设备生命周期事件: deviceId={}, action={}", event.getDeviceId(), event.getAction());

            switch (event.getAction()) {
                case REGISTER -> deviceAuthService.register(event.getDeviceId());
                case UNREGISTER -> deviceAuthService.unregister(event.getDeviceId());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理设备生命周期事件失败，触发重试: {}", message, e);
            throw new KafkaConsumeFailedException("设备生命周期事件处理失败", e);
        }
    }
}
