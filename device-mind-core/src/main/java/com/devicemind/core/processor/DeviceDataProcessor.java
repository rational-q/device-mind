package com.devicemind.core.processor;

import com.devicemind.common.kafka.model.DeviceDataPoint;

import java.util.List;

/**
 * 设备数据处理器接口（策略模式）
 * <p>
 * 每种产品类型对应一个 Processor 实现，负责将该产品的原始数据点
 * 写入时序库并更新设备影子
 */
public interface DeviceDataProcessor {

    /**
     * 处理设备数据点
     *
     * @param dataPoints 设备数据点列表
     */
    void process(List<DeviceDataPoint> dataPoints);

    /**
     * 返回支持的产品标识（productKey）
     *
     * @return productKey，如 "TEMP_SENSOR_V1"
     */
    String supportedProductKey();
}
