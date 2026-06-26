package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmThingServiceParam;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务参数 Mapper — 操作 MySQL dm_thing_service_param 表
 */
@Mapper
public interface DmThingServiceParamMapper extends BaseMapper<DmThingServiceParam> {
}
