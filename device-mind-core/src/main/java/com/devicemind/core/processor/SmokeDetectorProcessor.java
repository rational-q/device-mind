package com.devicemind.core.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmokeDetectorProcessor extends AbstractDeviceDataProcessor {
    @Override
    public String supportedProductKey() {
        return "SMOKE_DETECTOR_V1";
    }
}
