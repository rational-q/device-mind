package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmThingService;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物模型服务 Mapper — 操作 MySQL dm_thing_service 表
 */
@Mapper
public interface DmThingServiceMapper extends BaseMapper<DmThingService> {
}
