package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.model.entity.DmDeviceData;
import com.devicemind.core.persistence.mapper.timescale.DmDeviceDataMapper;
import com.devicemind.core.stdsvc.intf.IDmDeviceDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备数据服务
 * <p>
 * 负责将设备上报的时序数据批量写入 TimescaleDB，
 * 继承 MyBatis-Plus ServiceImpl 以使用 saveBatch() 等方法
 */
@Service
@Slf4j
public class DmDeviceDataService extends ServiceImpl<DmDeviceDataMapper, DmDeviceData> implements IDmDeviceDataService {

    /**
     * 批量保存设备数据点到 TimescaleDB 的 DEVICE_DATA 表中
     *
     * @param points 设备数据点列表
     */
    @Override
    public void saveData(List<DeviceDataPoint> points) {
        if (points == null || points.isEmpty()) {
            log.warn("saveData 收到空列表，跳过写入");
            return;
        }

        // DTO → Entity 转换（非数值类型返回 null，自动过滤）
        List<DmDeviceData> entities = points.stream()
                .map(p -> DmDeviceData.from(p.getDeviceId(), p.getAttrName(), p.getValue(), p.getTimestamp()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try {
            // 使用 MyBatis-Plus saveBatch 批量插入（默认每批 1000 条）
            saveBatch(entities);
            log.info("成功写入 {} 条设备数据到 TimescaleDB", entities.size());
        } catch (Exception e) {
            log.error("批量写入 TimescaleDB 失败，数据点数: {}", entities.size(), e);
            throw new ServiceException("设备数据写入失败", e);
        }
    }
}
