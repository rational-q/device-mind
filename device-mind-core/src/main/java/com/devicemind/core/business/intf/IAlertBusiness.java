package com.devicemind.core.business.intf;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.dto.AlertPageQueryDTO;
import com.devicemind.core.model.vo.AlertVO;

public interface IAlertBusiness {

    Page<AlertVO> listPage(AlertPageQueryDTO query);
    AlertVO getById(Long id);
    void confirm(Long id);
    void resolve(Long id);
}
