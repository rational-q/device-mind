package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmAlert;
import com.devicemind.core.persistence.mapper.mysql.DmAlertMapper;
import com.devicemind.core.stdsvc.intf.IDmAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 告警事件服务 — 操作 MySQL dm_alert 表
 */
@Service
@Slf4j
public class DmAlertServiceImpl extends ServiceImpl<DmAlertMapper, DmAlert> implements IDmAlertService {
}
