package com.devicemind.core.business.impl;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.ISceneBusiness;
import com.devicemind.core.model.dto.SceneCreateDTO;
import com.devicemind.core.model.dto.SceneUpdateDTO;
import com.devicemind.core.model.entity.DmScene;
import com.devicemind.core.model.entity.DmSceneLog;
import com.devicemind.core.model.vo.SceneLogVO;
import com.devicemind.core.model.vo.SceneVO;
import com.devicemind.core.stdsvc.intf.IDmSceneLogService;
import com.devicemind.core.stdsvc.intf.IDmSceneService;
import com.devicemind.common.utils.SnowflakeId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SceneBusiness implements ISceneBusiness {

    @Autowired
    private IDmSceneService sceneService;
    @Autowired
    private IDmSceneLogService sceneLogService;
    @Autowired
    private CacheManager cacheManager;

    /** 清空场景缓存（processor 用 cacheManager.getCache("scenes").get("enabled", loader) 编程式缓存） */
    private void evictSceneCache() {
        Cache cache = cacheManager.getCache("scenes");
        if (cache != null) cache.clear();
    }

    @Override
    public Page<SceneVO> listPage(int pageNum, int pageSize) {
        Page<DmScene> page = sceneService.page(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<DmScene>().orderByDesc(DmScene::getCreatedDate));
        Page<SceneVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public SceneVO getById(Long id) {
        DmScene scene = sceneService.getById(id);
        if (scene == null) throw new ServiceException(404, "场景不存在");
        return toVO(scene);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneCreateDTO dto) {
        DmScene scene = new DmScene();
        scene.setId(snowflakeId());
        scene.setName(dto.getName());
        scene.setDescription(dto.getDescription());
        scene.setProductId(dto.getProductId());
        scene.setConditions(dto.getConditions());
        scene.setActions(dto.getActions());
        scene.setEnabled(true);
        sceneService.save(scene);
        evictSceneCache();
        log.info("场景创建成功: id={}, name={}", scene.getId(), dto.getName());
        return scene.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SceneUpdateDTO dto) {
        DmScene scene = sceneService.getById(id);
        if (scene == null) throw new ServiceException(404, "场景不存在");
        if (dto.getName() != null) scene.setName(dto.getName());
        if (dto.getDescription() != null) scene.setDescription(dto.getDescription());
        if (dto.getConditions() != null) scene.setConditions(dto.getConditions());
        if (dto.getActions() != null) scene.setActions(dto.getActions());
        sceneService.updateById(scene);
        evictSceneCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!sceneService.removeById(id)) {
            throw new ServiceException(404, "场景不存在");
        }
        evictSceneCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggle(Long id) {
        DmScene scene = sceneService.getById(id);
        if (scene == null) throw new ServiceException(404, "场景不存在");
        scene.setEnabled(!Boolean.TRUE.equals(scene.getEnabled()));
        sceneService.updateById(scene);
        evictSceneCache();
    }

    @Override
    public Page<SceneLogVO> listLogPage(Long sceneId, int pageNum, int pageSize) {
        LambdaQueryWrapper<DmSceneLog> wrapper = new LambdaQueryWrapper<DmSceneLog>()
                .orderByDesc(DmSceneLog::getCreatedDate);
        if (sceneId != null) {
            wrapper.eq(DmSceneLog::getSceneId, sceneId);
        }
        Page<DmSceneLog> page = sceneLogService.page(Page.of(pageNum, pageSize), wrapper);
        Page<SceneLogVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toLogVO).toList());
        return voPage;
    }

    private SceneVO toVO(DmScene scene) {
        SceneVO vo = new SceneVO();
        BeanUtils.copyProperties(scene, vo);
        return vo;
    }

    private SceneLogVO toLogVO(DmSceneLog logEntry) {
        SceneLogVO vo = new SceneLogVO();
        BeanUtils.copyProperties(logEntry, vo);
        return vo;
    }

    private long snowflakeId() {
        return SnowflakeId.nextId();
    }
}
