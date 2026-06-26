package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.entity.DmAlert;
import com.devicemind.core.model.vo.AlertVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 告警事件 Mapper — 操作 MySQL dm_alert 表
 */
@Mapper
public interface DmAlertMapper extends BaseMapper<DmAlert> {

    /**
     * 分页查询告警列表（多条件筛选）
     */
    IPage<AlertVO> selectAlertPage(Page<AlertVO> page,
                                   @Param("deviceId") String deviceId,
                                   @Param("status") String status,
                                   @Param("level") String level,
                                   @Param("startTime") Long startTime,
                                   @Param("endTime") Long endTime);
}
