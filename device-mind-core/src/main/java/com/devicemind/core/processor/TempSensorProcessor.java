package com.devicemind.core.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TempSensorProcessor extends AbstractDeviceDataProcessor {
    @Override
    public String supportedProductKey() {
        return "TEMP_SENSOR_V1";
    }
}
