package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmThingServiceParam;
import com.devicemind.core.persistence.mapper.mysql.DmThingServiceParamMapper;
import com.devicemind.core.stdsvc.intf.IDmThingServiceParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 服务参数服务 — 操作 MySQL dm_thing_service_param 表
 */
@Service
@Slf4j
public class DmThingServiceParamServiceImpl extends ServiceImpl<DmThingServiceParamMapper, DmThingServiceParam> implements IDmThingServiceParamService {
}
