package com.devicemind.core.persistence.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devicemind.core.model.entity.DmAlertRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则 Mapper — 操作 MySQL dm_alert_rule 表
 */
@Mapper
public interface DmAlertRuleMapper extends BaseMapper<DmAlertRule> {
}
