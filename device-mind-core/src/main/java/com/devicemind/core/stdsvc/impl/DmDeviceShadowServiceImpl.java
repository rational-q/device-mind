package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmDeviceShadow;
import com.devicemind.core.persistence.mapper.mysql.DmDeviceShadowMapper;
import com.devicemind.core.stdsvc.intf.IDmDeviceShadowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备影子服务 — 操作 MySQL dm_device_shadow 表
 */
@Service
@Slf4j
public class DmDeviceShadowServiceImpl extends ServiceImpl<DmDeviceShadowMapper, DmDeviceShadow> implements IDmDeviceShadowService {
}
