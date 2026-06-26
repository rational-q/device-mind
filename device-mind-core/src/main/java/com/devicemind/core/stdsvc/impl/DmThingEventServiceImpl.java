package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmThingEvent;
import com.devicemind.core.persistence.mapper.mysql.DmThingEventMapper;
import com.devicemind.core.stdsvc.intf.IDmThingEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 物模型事件服务 — 操作 MySQL dm_thing_event 表
 */
@Service
@Slf4j
public class DmThingEventServiceImpl extends ServiceImpl<DmThingEventMapper, DmThingEvent> implements IDmThingEventService {
}
