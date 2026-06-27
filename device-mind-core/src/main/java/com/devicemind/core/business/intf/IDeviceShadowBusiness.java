package com.devicemind.core.business.intf;

import com.devicemind.core.model.dto.ShadowUpdateDTO;
import com.devicemind.core.model.vo.ShadowVO;

public interface IDeviceShadowBusiness {

    ShadowVO getShadow(String deviceId);

    void updateDesired(String deviceId, ShadowUpdateDTO dto);
}
