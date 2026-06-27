package com.devicemind.core.persistence.mapper.timescale;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.entity.DmDeviceData;
import com.devicemind.core.model.vo.DeviceDataVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 设备时序数据 Mapper — 操作 TimescaleDB DEVICE_DATA 表
 */
@Mapper
public interface DmDeviceDataMapper extends BaseMapper<DmDeviceData> {

    /**
     * 分页查询设备时序数据（时间范围筛选）
     */
    IPage<DeviceDataVO> selectDataPage(Page<DeviceDataVO> page,
                                       @Param("deviceId") String deviceId,
                                       @Param("attrName") String attrName,
                                       @Param("start") Long start,
                                       @Param("end") Long end);
}
