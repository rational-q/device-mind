package com.devicemind.core.kafka.consumer;

import com.devicemind.common.exception.KafkaConsumeFailedException;
import com.devicemind.common.kafka.model.DeviceResponseEvent;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 设备响应消费者 — Broker → Core（设备回执）
 * <p>
 * 收到设备回执后更新指令日志状态为 SUCCESS。
 * 异常抛出，由 CommonErrorHandler 处理重试。
 */
@Slf4j
@Component
public class DeviceResponseConsumer {
    @Autowired
    private IDmCommandLogService commandLogService;

    @KafkaListener(topics = "${kafka.topics.device-response}", groupId = "${spring.kafka.consumer.group-id:core-group}")
    public void onMessage(String message, Acknowledgment ack) {
        try {
            DeviceResponseEvent event = JsonUtil.fromJson(message, DeviceResponseEvent.class);
            log.info("收到设备回执: deviceId={}, command={}", event.getDeviceId(), event.getCommand());

            DmCommandLog cmdLog = commandLogService.lambdaQuery()
                    .eq(DmCommandLog::getIdempotencyKey, event.getIdempotencyKey())
                    .one();
            if (cmdLog != null) {
                cmdLog.setStatus("SUCCESS");
                commandLogService.updateById(cmdLog);
                log.info("指令状态更新为 SUCCESS: id={}", cmdLog.getId());
            } else {
                log.warn("未找到对应指令日志: idempotencyKey={}", event.getIdempotencyKey());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理设备回执失败，触发重试: {}", message, e);
            throw new KafkaConsumeFailedException("设备回执处理失败", e);
        }
    }
}
