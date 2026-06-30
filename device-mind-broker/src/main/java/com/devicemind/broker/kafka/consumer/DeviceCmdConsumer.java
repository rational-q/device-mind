package com.devicemind.broker.kafka.consumer;

import com.devicemind.broker.service.CommandService;
import com.devicemind.common.exception.KafkaConsumeFailedException;
import com.devicemind.common.kafka.model.DeviceCommandEvent;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 设备指令消费者 — Core → Broker（接收指令并下发给设备）
 * <p>
 * 异常抛出，由 CommonErrorHandler 处理重试。
 * 设备离线时抛出异常不 ack，等设备上线后 CommandRetrySupport 会重新投递。
 */
@Slf4j
@Service
public class DeviceCmdConsumer {
    @Autowired
    private CommandService commandService;

    @KafkaListener(topics = "${kafka.topics.device-command}", groupId = "${spring.kafka.consumer.group-id:broker-group}")
    public void onMessage(String message, Acknowledgment ack) {
        try {
            DeviceCommandEvent event = JsonUtil.fromJson(message, DeviceCommandEvent.class);
            log.info("收到设备指令: deviceId={}, command={}", event.getDeviceId(), event.getCommand());

            String topic = "device/command/" + event.getDeviceId();
            String payload = JsonUtil.toJson(Map.of(
                    "command", event.getCommand(),
                    "params", event.getParams() != null ? event.getParams() : "",
                    "idempotencyKey", event.getIdempotencyKey()
            ));

            boolean sent = commandService.sendCommand(event.getDeviceId(), topic, payload);
            if (sent) {
                log.info("指令下发成功: deviceId={}, command={}", event.getDeviceId(), event.getCommand());
                ack.acknowledge();
            } else {
                // 设备离线 → 抛出异常触发重试（CommandRetrySupport 会在设备上线时补投）
                log.warn("设备离线，指令下发失败（将重试）: deviceId={}, command={}",
                        event.getDeviceId(), event.getCommand());
                throw new KafkaConsumeFailedException("设备离线: " + event.getDeviceId());
            }
        } catch (KafkaConsumeFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("处理设备指令失败，触发重试: {}", message, e);
            throw new KafkaConsumeFailedException("设备指令处理失败", e);
        }
    }
}
