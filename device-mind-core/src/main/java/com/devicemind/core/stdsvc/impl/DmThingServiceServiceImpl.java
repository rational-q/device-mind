package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmThingService;
import com.devicemind.core.persistence.mapper.mysql.DmThingServiceMapper;
import com.devicemind.core.stdsvc.intf.IDmThingServiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 物模型服务定义服务 — 操作 MySQL dm_thing_service 表
 */
@Service
@Slf4j
public class DmThingServiceServiceImpl extends ServiceImpl<DmThingServiceMapper, DmThingService> implements IDmThingServiceService {
}
