package com.devicemind.core.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmartLockProcessor extends AbstractDeviceDataProcessor {
    @Override
    public String supportedProductKey() {
        return "SMART_LOCK_V1";
    }
}
