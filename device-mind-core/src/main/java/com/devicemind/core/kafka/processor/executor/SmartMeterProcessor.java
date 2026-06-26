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
 * 智能电表处理器 — productKey: SMART_METER_V1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartMeterProcessor implements DeviceDataProcessor {

    private final DeviceDataService deviceDataService;
    private final DeviceShadowService deviceShadowService;
    private final AlertEngine alertEngine;

    @Override
    public void process(List<DeviceDataPoint> dataPoints) {
        log.info("SmartMeterProcessor 处理 {} 条数据", dataPoints.size());
        deviceDataService.saveData(dataPoints);
        deviceShadowService.updateReported(dataPoints);
        alertEngine.evaluate(dataPoints, supportedProductKey());
    }

    @Override
    public String supportedProductKey() {
        return "SMART_METER_V1";
    }
}
