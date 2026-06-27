package com.devicemind.core.stdsvc.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devicemind.core.model.entity.DmScene;
import com.devicemind.core.persistence.mapper.mysql.DmSceneMapper;
import com.devicemind.core.stdsvc.intf.IDmSceneService;
import org.springframework.stereotype.Service;

@Service
public class DmSceneServiceImpl extends ServiceImpl<DmSceneMapper, DmScene> implements IDmSceneService {
}
