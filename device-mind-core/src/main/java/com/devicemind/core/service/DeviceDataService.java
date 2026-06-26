package com.devicemind.core.service;

import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.stdsvc.intf.IDmDeviceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备数据存储服务
 * <p>
 * 委托 {@link IDmDeviceDataService} 将设备时序数据批量写入 TimescaleDB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final IDmDeviceDataService dmDeviceDataService;

    /**
     * 批量保存设备数据点
     *
     * @param points 设备数据点列表
     */
    public void saveData(List<DeviceDataPoint> points) {
        dmDeviceDataService.saveData(points);
    }
}
