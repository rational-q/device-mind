package com.devicemind.core.kafka.processor;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备数据处理器工厂
 * <p>
 * 自动收集所有 {@link DeviceDataProcessor} 实现类，
 * 按 {@link DeviceDataProcessor#supportedProductKey()} 建立映射，按需返回。
 */
@Slf4j
@Component
public class DeviceDataProcessorFactory {

    private final List<DeviceDataProcessor> processors;
    private final Map<String, DeviceDataProcessor> processorMap = new HashMap<>();

    public DeviceDataProcessorFactory(List<DeviceDataProcessor> processors) {
        this.processors = processors;
    }

    @PostConstruct
    public void init() {
        for (DeviceDataProcessor processor : processors) {
            String productKey = processor.supportedProductKey();
            processorMap.put(productKey, processor);
            log.info("注册设备数据处理器: productKey={} → {}", productKey, processor.getClass().getSimpleName());
        }
    }

    /**
     * 根据 productKey 获取对应的数据处理器
     *
     * @param productKey 产品标识
     * @return 对应的处理器，未匹配时返回 null
     */
    public DeviceDataProcessor getProcessor(String productKey) {
        DeviceDataProcessor processor = processorMap.get(productKey);
        if (processor == null) {
            log.warn("未找到匹配的处理器: productKey={}", productKey);
        }
        return processor;
    }
}
