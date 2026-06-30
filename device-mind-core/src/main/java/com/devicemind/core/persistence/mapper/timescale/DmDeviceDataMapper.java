package com.devicemind.core.persistence.mapper.timescale;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmDeviceData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DmDeviceDataMapper extends BaseMapper<DmDeviceData> {
}
