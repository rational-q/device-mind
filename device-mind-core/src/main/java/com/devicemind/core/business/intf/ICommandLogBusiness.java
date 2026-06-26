package com.devicemind.core.business.intf;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.dto.CommandLogPageQueryDTO;
import com.devicemind.core.model.vo.CommandLogVO;

public interface ICommandLogBusiness {

    Page<CommandLogVO> listPage(CommandLogPageQueryDTO query);
    CommandLogVO getById(Long id);
}
