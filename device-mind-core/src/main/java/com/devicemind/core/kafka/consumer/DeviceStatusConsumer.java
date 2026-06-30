package com.devicemind.core.kafka.consumer;

import com.devicemind.common.exception.KafkaConsumeFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.common.kafka.model.DeviceStatusEvent;
import com.devicemind.core.business.intf.IDeviceBusiness;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 设备状态消费者 — Broker → Core（online / offline）
 * <p>
 * 异常抛出，由 CommonErrorHandler 处理重试。
 */
@Slf4j
@Component
public class DeviceStatusConsumer {
    @Autowired
    private IDeviceBusiness deviceBusiness;

    @KafkaListener(topics = "${kafka.topics.device-status}", groupId = "${spring.kafka.consumer.group-id:core-group}")
    public void onMessage(String message, Acknowledgment ack) {
        try {
            DeviceStatusEvent event = JsonUtil.fromJson(message, DeviceStatusEvent.class);
            log.info("收到设备状态事件: deviceId={}, status={}", event.getDeviceId(), event.getStatus());
            deviceBusiness.updateStatusByDeviceId(event.getDeviceId(), event.getStatus());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理设备状态事件失败，触发重试: {}", message, e);
            throw new KafkaConsumeFailedException("设备状态处理失败", e);
        }
    }
}
