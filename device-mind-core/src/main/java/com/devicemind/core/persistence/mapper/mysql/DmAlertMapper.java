package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmAlert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DmAlertMapper extends BaseMapper<DmAlert> {
}
