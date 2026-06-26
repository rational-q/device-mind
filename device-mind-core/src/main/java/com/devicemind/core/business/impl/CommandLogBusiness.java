package com.devicemind.core.business.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.ICommandLogBusiness;
import com.devicemind.core.model.dto.CommandLogPageQueryDTO;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.model.vo.CommandLogVO;
import com.devicemind.core.persistence.mapper.mysql.DmCommandLogMapper;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandLogBusiness implements ICommandLogBusiness {

    private final IDmCommandLogService commandLogService;
    private final DmCommandLogMapper commandLogMapper;

    @Override
    public Page<CommandLogVO> listPage(CommandLogPageQueryDTO query) {
        Page<CommandLogVO> page = Page.of(query.getPageNum(), query.getPageSize());
        return (Page<CommandLogVO>) commandLogMapper.selectCommandLogPage(
                page, query.getDeviceId(), query.getCommand(), query.getStatus());
    }

    @Override
    public CommandLogVO getById(Long id) {
        DmCommandLog log = commandLogService.getById(id);
        if (log == null) throw new ServiceException(404, "指令记录不存在");
        CommandLogVO vo = new CommandLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
