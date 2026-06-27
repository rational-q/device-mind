package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmDeviceShadow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备影子 Mapper — 操作 MySQL dm_device_shadow 表
 */
@Mapper
public interface DmDeviceShadowMapper extends BaseMapper<DmDeviceShadow> {
}
