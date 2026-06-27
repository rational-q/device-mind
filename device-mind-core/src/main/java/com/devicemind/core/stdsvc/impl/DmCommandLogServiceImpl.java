package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.persistence.mapper.mysql.DmCommandLogMapper;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 指令记录服务 — 操作 MySQL dm_command_log 表
 */
@Service
@Slf4j
public class DmCommandLogServiceImpl extends ServiceImpl<DmCommandLogMapper, DmCommandLog> implements IDmCommandLogService {
}
