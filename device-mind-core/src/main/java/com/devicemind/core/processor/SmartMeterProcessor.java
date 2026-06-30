package com.devicemind.core.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmartMeterProcessor extends AbstractDeviceDataProcessor {
    @Override
    public String supportedProductKey() {
        return "SMART_METER_V1";
    }
}
