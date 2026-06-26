package com.devicemind.core.business.intf;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.vo.DeviceVO;

public interface IDeviceBusiness {

    Page<DeviceVO> listPage(DevicePageQueryDTO query);

    DeviceVO getById(Long id);

    void create(DeviceCreateDTO dto);

    void update(Long id, DeviceUpdateDTO dto);

    void delete(Long id);

    void updateStatus(Long id, DeviceStatusUpdateDTO dto);
}
