package com.devicemind.core.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IndustrialPLCProcessor extends AbstractDeviceDataProcessor {
    @Override
    public String supportedProductKey() {
        return "INDUSTRIAL_PLC_V1";
    }
}
