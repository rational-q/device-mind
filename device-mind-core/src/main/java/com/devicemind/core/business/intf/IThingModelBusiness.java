package com.devicemind.core.business.intf;

import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.vo.*;

import java.util.List;

public interface IThingModelBusiness {

    // 属性
    List<ThingAttributeVO> listAttributes(Long productId);
    void createAttribute(Long productId, ThingAttributeCreateDTO dto);
    void updateAttribute(Long productId, Long id, ThingAttributeUpdateDTO dto);
    void deleteAttribute(Long productId, Long id);

    // 服务
    List<ThingServiceVO> listServices(Long productId);
    void createService(Long productId, ThingServiceCreateDTO dto);
    void updateService(Long productId, Long id, ThingServiceUpdateDTO dto);
    void deleteService(Long productId, Long id);

    // 事件
    List<ThingEventVO> listEvents(Long productId);
    void createEvent(Long productId, ThingEventCreateDTO dto);
    void updateEvent(Long productId, Long id, ThingEventUpdateDTO dto);
    void deleteEvent(Long productId, Long id);
}
