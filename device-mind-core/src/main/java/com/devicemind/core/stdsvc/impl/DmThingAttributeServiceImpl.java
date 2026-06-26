package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmThingAttribute;
import com.devicemind.core.persistence.mapper.mysql.DmThingAttributeMapper;
import com.devicemind.core.stdsvc.intf.IDmThingAttributeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 物模型属性服务 — 操作 MySQL dm_thing_attribute 表
 */
@Service
@Slf4j
public class DmThingAttributeServiceImpl extends ServiceImpl<DmThingAttributeMapper, DmThingAttribute> implements IDmThingAttributeService {
}
