package com.devicemind.core.business.intf;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.vo.AlertRuleVO;

public interface IAlertRuleBusiness {

    Page<AlertRuleVO> listPage(AlertRulePageQueryDTO query);
    AlertRuleVO getById(Long id);
    void create(AlertRuleCreateDTO dto);
    void update(Long id, AlertRuleUpdateDTO dto);
    void delete(Long id);
    void toggle(Long id);
}
