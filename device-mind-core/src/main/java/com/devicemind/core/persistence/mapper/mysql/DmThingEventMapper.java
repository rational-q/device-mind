package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmThingEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物模型事件 Mapper — 操作 MySQL dm_thing_event 表
 */
@Mapper
public interface DmThingEventMapper extends BaseMapper<DmThingEvent> {
}
