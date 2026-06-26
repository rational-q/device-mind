package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.model.vo.CommandLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 指令记录 Mapper — 操作 MySQL dm_command_log 表
 */
@Mapper
public interface DmCommandLogMapper extends BaseMapper<DmCommandLog> {

    /**
     * 分页查询指令日志（多条件筛选）
     */
    IPage<CommandLogVO> selectCommandLogPage(Page<CommandLogVO> page,
                                             @Param("deviceId") String deviceId,
                                             @Param("command") String command,
                                             @Param("status") String status);
}
