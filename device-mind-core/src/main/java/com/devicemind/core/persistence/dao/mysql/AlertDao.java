package com.devicemind.core.persistence.dao.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.vo.AlertVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 告警 DAO — 自定义 SQL
 */
@Mapper
public interface AlertDao {

    IPage<AlertVO> selectAlertPage(Page<AlertVO> page,
                                   @Param("deviceId") String deviceId,
                                   @Param("status") String status,
                                   @Param("level") String level,
                                   @Param("startTime") Long startTime,
                                   @Param("endTime") Long endTime);
}
