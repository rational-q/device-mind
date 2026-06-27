package com.devicemind.core.business.intf;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.dto.SceneCreateDTO;
import com.devicemind.core.model.dto.SceneUpdateDTO;
import com.devicemind.core.model.vo.SceneLogVO;
import com.devicemind.core.model.vo.SceneVO;

public interface ISceneBusiness {

    Page<SceneVO> listPage(int pageNum, int pageSize);
    SceneVO getById(Long id);
    Long create(SceneCreateDTO dto);
    void update(Long id, SceneUpdateDTO dto);
    void delete(Long id);
    void toggle(Long id);

    Page<SceneLogVO> listLogPage(Long sceneId, int pageNum, int pageSize);
}
