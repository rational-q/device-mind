package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmCommandLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DmCommandLogMapper extends BaseMapper<DmCommandLog> {
}
