package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.model.vo.DeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 设备 Mapper — 操作 MySQL dm_device 表
 */
@Mapper
public interface DmDeviceMapper extends BaseMapper<DmDevice> {

    /**
     * 分页查询设备列表（联表查询产品名称）
     */
    IPage<DeviceVO> selectDevicePage(Page<DeviceVO> page,
                                     @Param("deviceId") String deviceId,
                                     @Param("productId") Long productId,
                                     @Param("status") String status);

    /**
     * 统计某产品下的设备数量
     */
    long countByProductId(@Param("productId") Long productId);
}
