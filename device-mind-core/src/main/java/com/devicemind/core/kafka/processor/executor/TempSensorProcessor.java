package com.devicemind.core.kafka.processor.executor;

import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.service.AlertEngine;
import com.devicemind.core.service.DeviceDataService;
import com.devicemind.core.service.DeviceShadowService;
import com.devicemind.core.kafka.processor.DeviceDataProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 温度传感器处理器 — productKey: TEMP_SENSOR_V1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempSensorProcessor implements DeviceDataProcessor {

    private final DeviceDataService deviceDataService;
    private final DeviceShadowService deviceShadowService;
    private final AlertEngine alertEngine;

    @Override
    public void process(List<DeviceDataPoint> dataPoints) {
        log.info("TempSensorProcessor 处理 {} 条数据", dataPoints.size());
        deviceDataService.saveData(dataPoints);
        deviceShadowService.updateReported(dataPoints);
        alertEngine.evaluate(dataPoints, supportedProductKey());
    }

    @Override
    public String supportedProductKey() {
        return "TEMP_SENSOR_V1";
    }
}
