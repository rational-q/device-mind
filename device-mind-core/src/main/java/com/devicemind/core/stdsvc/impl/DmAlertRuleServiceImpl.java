package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmAlertRule;
import com.devicemind.core.persistence.mapper.mysql.DmAlertRuleMapper;
import com.devicemind.core.stdsvc.intf.IDmAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 告警规则服务 — 操作 MySQL dm_alert_rule 表
 */
@Service
@Slf4j
public class DmAlertRuleServiceImpl extends ServiceImpl<DmAlertRuleMapper, DmAlertRule> implements IDmAlertRuleService {
}
