package com.devicemind.core.persistence.dao.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.vo.CommandLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 指令日志 DAO — 自定义 SQL
 */
@Mapper
public interface CommandLogDao {

    IPage<CommandLogVO> selectCommandLogPage(Page<CommandLogVO> page,
                                             @Param("deviceId") String deviceId,
                                             @Param("command") String command,
                                             @Param("status") String status);
}
