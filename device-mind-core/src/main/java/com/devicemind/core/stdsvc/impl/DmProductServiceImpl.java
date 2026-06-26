package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmProductDO;
import com.devicemind.core.persistence.mapper.mysql.DmProductMapper;
import com.devicemind.core.stdsvc.intf.IDmProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 产品服务 — 操作 MySQL dm_product 表
 */
@Service
@Slf4j
public class DmProductServiceImpl extends ServiceImpl<DmProductMapper, DmProductDO> implements IDmProductService {
}
