package com.devicemind.core.business.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.IAlertBusiness;
import com.devicemind.core.model.dto.AlertPageQueryDTO;
import com.devicemind.core.model.entity.DmAlert;
import com.devicemind.core.model.vo.AlertVO;
import com.devicemind.core.persistence.mapper.mysql.DmAlertMapper;
import com.devicemind.core.stdsvc.intf.IDmAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertBusiness implements IAlertBusiness {

    private final IDmAlertService alertService;
    private final DmAlertMapper alertMapper;

    @Override
    public Page<AlertVO> listPage(AlertPageQueryDTO query) {
        Page<AlertVO> page = Page.of(query.getPageNum(), query.getPageSize());
        return (Page<AlertVO>) alertMapper.selectAlertPage(
                page, query.getDeviceId(), query.getStatus(), query.getLevel(),
                query.getStartTime(), query.getEndTime());
    }

    @Override
    public AlertVO getById(Long id) {
        DmAlert alert = alertService.getById(id);
        if (alert == null) throw new ServiceException(404, "告警不存在");
        AlertVO vo = new AlertVO();
        org.springframework.beans.BeanUtils.copyProperties(alert, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        DmAlert alert = alertService.getById(id);
        if (alert == null) throw new ServiceException(404, "告警不存在");
        if (!"TRIGGERED".equals(alert.getStatus())) {
            throw new ServiceException("仅可确认未处理的告警");
        }
        DmAlert update = new DmAlert();
        update.setId(id);
        update.setStatus("CONFIRMED");
        update.setConfirmedAt(new Date());
        alertService.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long id) {
        DmAlert alert = alertService.getById(id);
        if (alert == null) throw new ServiceException(404, "告警不存在");
        if ("RESOLVED".equals(alert.getStatus())) {
            throw new ServiceException("告警已恢复");
        }
        DmAlert update = new DmAlert();
        update.setId(id);
        update.setStatus("RESOLVED");
        update.setResolvedAt(new Date());
        alertService.updateById(update);
    }
}
