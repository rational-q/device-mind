package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmDevice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DmDeviceMapper extends BaseMapper<DmDevice> {
}
