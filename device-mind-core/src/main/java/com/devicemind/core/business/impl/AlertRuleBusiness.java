package com.devicemind.core.business.impl;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.IAlertRuleBusiness;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.entity.DmAlertRule;
import com.devicemind.core.model.vo.AlertRuleVO;
import com.devicemind.core.stdsvc.intf.IDmAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class AlertRuleBusiness implements IAlertRuleBusiness {

    @Autowired
    private IDmAlertRuleService alertRuleService;

    @Override
    public Page<AlertRuleVO> listPage(AlertRulePageQueryDTO query) {
        LambdaQueryWrapper<DmAlertRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getDeviceType())) {
            wrapper.eq(DmAlertRule::getDeviceType, query.getDeviceType());
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(DmAlertRule::getLevel, query.getLevel());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(DmAlertRule::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(DmAlertRule::getCreatedDate);

        Page<DmAlertRule> page = Page.of(query.getPageNum(), query.getPageSize());
        alertRuleService.page(page, wrapper);

        List<AlertRuleVO> vos = page.getRecords().stream().map(e -> {
            AlertRuleVO vo = new AlertRuleVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        Page<AlertRuleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public AlertRuleVO getById(Long id) {
        DmAlertRule rule = alertRuleService.getById(id);
        if (rule == null) throw new ServiceException(404, "告警规则不存在");
        AlertRuleVO vo = new AlertRuleVO();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(AlertRuleCreateDTO dto) {
        DmAlertRule entity = new DmAlertRule();
        BeanUtils.copyProperties(dto, entity);
        alertRuleService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AlertRuleUpdateDTO dto) {
        DmAlertRule existing = alertRuleService.getById(id);
        if (existing == null) throw new ServiceException(404, "告警规则不存在");
        BeanUtils.copyProperties(dto, existing, "id");
        alertRuleService.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (alertRuleService.getById(id) == null) throw new ServiceException(404, "告警规则不存在");
        alertRuleService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggle(Long id) {
        DmAlertRule rule = alertRuleService.getById(id);
        if (rule == null) throw new ServiceException(404, "告警规则不存在");
        DmAlertRule update = new DmAlertRule();
        update.setId(id);
        update.setEnabled(!Boolean.TRUE.equals(rule.getEnabled()));
        alertRuleService.updateById(update);
    }
}
