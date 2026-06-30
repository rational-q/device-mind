package com.devicemind.core.business.impl;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.ICommandLogBusiness;
import com.devicemind.core.model.dto.CommandLogPageQueryDTO;
import com.devicemind.core.model.entity.DmCommandLog;
import com.devicemind.core.model.vo.CommandLogVO;
import com.devicemind.core.persistence.dao.mysql.CommandLogDao;
import com.devicemind.core.stdsvc.intf.IDmCommandLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommandLogBusiness implements ICommandLogBusiness {

    @Autowired
    private IDmCommandLogService commandLogService;
    @Autowired
    private CommandLogDao commandLogDao;

    @Override
    public Page<CommandLogVO> listPage(CommandLogPageQueryDTO query) {
        Page<CommandLogVO> page = Page.of(query.getPageNum(), query.getPageSize());
        return (Page<CommandLogVO>) commandLogDao.selectCommandLogPage(
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
