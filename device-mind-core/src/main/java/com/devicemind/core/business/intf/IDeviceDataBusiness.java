package com.devicemind.core.business.intf;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.dto.DeviceDataQueryDTO;
import com.devicemind.core.model.dto.DeviceDataRequest;
import com.devicemind.core.model.vo.DeviceDataVO;

public interface IDeviceDataBusiness {

    void processDeviceData(DeviceDataRequest request);

    Page<DeviceDataVO> queryData(DeviceDataQueryDTO query);
}
