package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmThingAttribute;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物模型属性 Mapper — 操作 MySQL dm_thing_attribute 表
 */
@Mapper
public interface DmThingAttributeMapper extends BaseMapper<DmThingAttribute> {
}
