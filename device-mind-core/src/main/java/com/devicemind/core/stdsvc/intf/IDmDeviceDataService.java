package com.devicemind.core.stdsvc.intf;

import com.baomidou.mybatisplus.extension.service.IService;
import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.model.entity.DmDeviceData;

import java.util.List;

public interface IDmDeviceDataService extends IService<DmDeviceData> {

    /**
     * 批量保存设备数据点到 TimescaleDB
     *
     * @param points 设备数据点列表
     */
    void saveData(List<DeviceDataPoint> points);
}
