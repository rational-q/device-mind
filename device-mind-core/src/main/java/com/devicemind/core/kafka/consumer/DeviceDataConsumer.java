package com.devicemind.core.kafka.consumer;

import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.model.dto.DeviceDataRequest;
import com.devicemind.core.service.DeviceService;
import com.devicemind.core.kafka.processor.DeviceDataProcessor;
import com.devicemind.core.kafka.processor.DeviceDataProcessorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设备数据 Kafka 消费者
 * <p>
 * 从 topic "device-data" 消费设备上报数据，经过设备/产品信息补全后，
 * 路由到对应的数据处理器进行格式转换和入库。
 * <p>
 * 消息格式（与 {@link DeviceDataRequest} 一致）：
 * <pre>
 * {
 *   "deviceId": "A-102",
 *   "timestamp": 1718200000,
 *   "attrs": {
 *     "temperature": 30.5,
 *     "humidity": 65.0
 *   }
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceDataConsumer {

    private final ObjectMapper objectMapper;
    private final DeviceService deviceService;
    private final DeviceDataProcessorFactory processorFactory;

    /**
     * 消费设备数据消息
     *
     * @param message JSON 字符串
     */
    @KafkaListener(topics = "${kafka.topics.device-data}", groupId = "${spring.kafka.consumer.group-id:core-group}")
    public void onMessage(String message) {
        try {
            // 1. 解析消息
            DeviceDataRequest request = objectMapper.readValue(message, DeviceDataRequest.class);
            String deviceId = request.getDeviceId();
            log.info("收到 Kafka 消息: deviceId={}, attrs={}", deviceId, request.getAttrs().keySet());

            // 2. 查询 productKey
            String productKey = deviceService.getProductKeyByDeviceId(deviceId);
            if (productKey == null) {
                log.warn("无法获取 productKey，丢弃消息: deviceId={}", deviceId);
                return;
            }

            // 3. 获取对应的处理器
            DeviceDataProcessor processor = processorFactory.getProcessor(productKey);
            if (processor == null) {
                log.warn("未找到处理器，丢弃消息: deviceId={}, productKey={}", deviceId, productKey);
                return;
            }

            // 4. DeviceDataRequest → List<DeviceDataPoint>
            long ts = request.getTimestamp() != null ? request.getTimestamp() : System.currentTimeMillis() / 1000;
            List<DeviceDataPoint> dataPoints = new ArrayList<>();
            for (Map.Entry<String, Object> entry : request.getAttrs().entrySet()) {
                dataPoints.add(new DeviceDataPoint(deviceId, entry.getKey(), entry.getValue(), ts));
            }

            // 5. 处理器执行
            processor.process(dataPoints);
            log.info("消息处理完成: deviceId={}, productKey={}, processor={}",
                    deviceId, productKey, processor.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("处理 Kafka 消息失败: {}", message, e);
        }
    }
}
