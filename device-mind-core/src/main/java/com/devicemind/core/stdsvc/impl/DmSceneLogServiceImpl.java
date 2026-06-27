package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmSceneLog;
import com.devicemind.core.persistence.mapper.mysql.DmSceneLogMapper;
import com.devicemind.core.stdsvc.intf.IDmSceneLogService;
import org.springframework.stereotype.Service;

@Service
public class DmSceneLogServiceImpl extends ServiceImpl<DmSceneLogMapper, DmSceneLog> implements IDmSceneLogService {
}
