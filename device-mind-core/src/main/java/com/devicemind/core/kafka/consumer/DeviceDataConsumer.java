package com.devicemind.core.kafka.consumer;

import com.devicemind.common.exception.KafkaConsumeFailedException;
import com.devicemind.common.kafka.model.DeviceDataPoint;
import com.devicemind.core.model.dto.DeviceDataRequest;
import com.devicemind.core.support.DeviceDataWebSocketHandler;
import com.devicemind.core.support.DeviceSupport;
import com.devicemind.core.processor.DeviceDataProcessor;
import com.devicemind.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 设备数据 Kafka 消费者（批量模式）
 * <p>
 * 每次 poll 拉取最多 {@code max-poll-records} 条消息，在同一批次内：
 * <ol>
 *   <li>解析所有消息，按 deviceId 分组</li>
 *   <li>每组内按数据时间戳排序 → 保证单设备数据有序入库</li>
 *   <li>按 productKey 路由到对应 Processor 批量写入 TSDB</li>
 *   <li>全部成功后统一 ack，任一条失败则整批重试</li>
 * </ol>
 * <p>
 * <b>性能</b>：单次 TSDB bulk insert 比逐条 insert 快 10-50 倍，
 * 即使设备 1 秒 1 条上报，每批 20 条的写入开销跟逐条差不多。
 */
@Slf4j
@Component
public class DeviceDataConsumer {
    @Autowired
    private DeviceSupport deviceService;
    private final Map<String, DeviceDataProcessor> processorMap = new ConcurrentHashMap<>();

    @Autowired
    private DeviceDataWebSocketHandler webSocketHandler;

    @Autowired
    public void setProcessors(List<DeviceDataProcessor> processors) {
        processorMap.putAll(processors.stream()
                .collect(Collectors.toMap(DeviceDataProcessor::supportedProductKey, p -> p)));
    }

    /**
     * 批量消费（batch=true），一次 poll 的所有消息合并处理、统一 ack。
     */
    @KafkaListener(topics = "${kafka.topics.device-data}",
            groupId = "${spring.kafka.consumer.group-id:core-group}",
            batch = "true")
    public void onMessage(List<String> messages, Acknowledgment ack) {
        if (messages.isEmpty()) {
            ack.acknowledge();
            return;
        }

        log.info("批量消费: {} 条消息", messages.size());

        // 1. 解析 + 按 deviceId 分组
        Map<String, List<DeviceDataPoint>> devicePoints = new HashMap<>();

        for (String message : messages) {
            try {
                DeviceDataRequest request = JsonUtil.fromJson(message, DeviceDataRequest.class);
                String deviceId = request.getDeviceId();
                long ts = request.getTimestamp() != null ? request.getTimestamp() : System.currentTimeMillis() / 1000;

                List<DeviceDataPoint> points = devicePoints.computeIfAbsent(deviceId, k -> new ArrayList<>());
                for (Map.Entry<String, Object> entry : request.getAttrs().entrySet()) {
                    points.add(new DeviceDataPoint(deviceId, entry.getKey(), entry.getValue(), ts));
                }
            } catch (Exception e) {
                log.error("消息反序列化失败，跳过: {}", message, e);
                // 格式错误的消息不阻塞整批
            }
        }

        // 2. 每组内按时间戳排序（保证同一设备数据顺序写入 TSDB）
        for (List<DeviceDataPoint> points : devicePoints.values()) {
            points.sort(Comparator.comparingLong(DeviceDataPoint::getTimestamp));
        }

        // 3. 按 productKey 路由到 Processor，批量写入 TSDB
        try {
            for (Map.Entry<String, List<DeviceDataPoint>> entry : devicePoints.entrySet()) {
                String deviceId = entry.getKey();
                List<DeviceDataPoint> points = entry.getValue();

                String productKey = deviceService.getProductKeyByDeviceId(deviceId);
                if (productKey == null) {
                    log.warn("无法获取 productKey，跳过 {} 条数据: deviceId={}", points.size(), deviceId);
                    continue;
                }

                DeviceDataProcessor processor = processorMap.get(productKey);
                if (processor == null) {
                    log.warn("未找到处理器，跳过 {} 条数据: deviceId={}, productKey={}",
                            points.size(), deviceId, productKey);
                    continue;
                }

                processor.process(points);
                log.debug("批次写入完成: deviceId={}, processor={}, points={}",
                        deviceId, processor.getClass().getSimpleName(), points.size());
            }

            // 4. WebSocket 广播（写完 TSDB 再广播，保证一致性）
            for (Map.Entry<String, List<DeviceDataPoint>> entry : devicePoints.entrySet()) {
                for (DeviceDataPoint dp : entry.getValue()) {
                    webSocketHandler.broadcastDeviceData(dp.getDeviceId(), dp.getAttrName(),
                            dp.getValue(), dp.getTimestamp());
                }
            }

            // 5. 全部成功，统一 ack
            ack.acknowledge();
            log.info("批量消费完成: {} 条消息，{} 个设备", messages.size(), devicePoints.size());

        } catch (Exception e) {
            log.error("批量处理失败，整批重试: {} 条消息", messages.size(), e);
            throw new KafkaConsumeFailedException("设备数据批量处理失败", e);
        }
    }
}
