package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmProductDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品 Mapper — 操作 MySQL dm_product 表
 */
@Mapper
public interface DmProductMapper extends BaseMapper<DmProductDO> {
}
