package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.persistence.mapper.mysql.DmDeviceMapper;
import com.devicemind.core.stdsvc.intf.IDmDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备服务 — 操作 MySQL dm_device 表
 */
@Service
@Slf4j
public class DmDeviceServiceImpl extends ServiceImpl<DmDeviceMapper, DmDevice> implements IDmDeviceService {
}
