package com.devicemind.core.business.impl;

import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.IThingModelBusiness;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.entity.*;
import com.devicemind.core.model.vo.*;
import com.devicemind.core.stdsvc.intf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThingModelBusiness implements IThingModelBusiness {

    private final IDmProductService productService;
    private final IDmThingAttributeService attributeService;
    private final IDmThingServiceService thingService;
    private final IDmThingServiceParamService paramService;
    private final IDmThingEventService eventService;

    private void checkProductExists(Long productId) {
        if (productService.getById(productId) == null) {
            throw new ServiceException(404, "产品不存在");
        }
    }

    // ==================== 属性 ====================

    @Override
    public List<ThingAttributeVO> listAttributes(Long productId) {
        checkProductExists(productId);
        return attributeService.lambdaQuery()
                .eq(DmThingAttribute::getProductId, productId)
                .list().stream().map(this::toAttrVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAttribute(Long productId, ThingAttributeCreateDTO dto) {
        checkProductExists(productId);
        DmThingAttribute entity = new DmThingAttribute();
        BeanUtils.copyProperties(dto, entity);
        entity.setProductId(productId);
        attributeService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAttribute(Long productId, Long id, ThingAttributeUpdateDTO dto) {
        DmThingAttribute existing = attributeService.lambdaQuery()
                .eq(DmThingAttribute::getProductId, productId)
                .eq(DmThingAttribute::getId, id).one();
        if (existing == null) throw new ServiceException(404, "属性不存在");
        BeanUtils.copyProperties(dto, existing, "id", "productId");
        attributeService.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttribute(Long productId, Long id) {
        DmThingAttribute existing = attributeService.lambdaQuery()
                .eq(DmThingAttribute::getProductId, productId)
                .eq(DmThingAttribute::getId, id).one();
        if (existing == null) throw new ServiceException(404, "属性不存在");
        attributeService.removeById(id);
    }

    // ==================== 服务 ====================

    @Override
    public List<ThingServiceVO> listServices(Long productId) {
        checkProductExists(productId);
        List<DmThingService> services = thingService.lambdaQuery()
                .eq(DmThingService::getProductId, productId).list();
        if (services.isEmpty()) return List.of();

        List<Long> serviceIds = services.stream().map(DmThingService::getId).toList();
        Map<Long, List<ThingServiceParamVO>> paramMap = paramService.lambdaQuery()
                .in(DmThingServiceParam::getServiceId, serviceIds).list()
                .stream().map(this::toParamVO)
                .collect(Collectors.groupingBy(ThingServiceParamVO::getServiceId));

        return services.stream().map(svc -> {
            ThingServiceVO vo = new ThingServiceVO();
            BeanUtils.copyProperties(svc, vo);
            vo.setParams(paramMap.getOrDefault(svc.getId(), List.of()));
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createService(Long productId, ThingServiceCreateDTO dto) {
        checkProductExists(productId);
        DmThingService svc = new DmThingService();
        BeanUtils.copyProperties(dto, svc, "params");
        svc.setProductId(productId);
        thingService.save(svc);

        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            List<DmThingServiceParam> params = dto.getParams().stream().map(p -> {
                DmThingServiceParam param = new DmThingServiceParam();
                BeanUtils.copyProperties(p, param);
                param.setServiceId(svc.getId());
                return param;
            }).toList();
            paramService.saveBatch(params);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateService(Long productId, Long id, ThingServiceUpdateDTO dto) {
        DmThingService existing = thingService.lambdaQuery()
                .eq(DmThingService::getProductId, productId)
                .eq(DmThingService::getId, id).one();
        if (existing == null) throw new ServiceException(404, "服务不存在");
        BeanUtils.copyProperties(dto, existing, "id", "productId");
        thingService.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteService(Long productId, Long id) {
        DmThingService existing = thingService.lambdaQuery()
                .eq(DmThingService::getProductId, productId)
                .eq(DmThingService::getId, id).one();
        if (existing == null) throw new ServiceException(404, "服务不存在");
        paramService.lambdaUpdate().eq(DmThingServiceParam::getServiceId, id).remove();
        thingService.removeById(id);
    }

    // ==================== 事件 ====================

    @Override
    public List<ThingEventVO> listEvents(Long productId) {
        checkProductExists(productId);
        return eventService.lambdaQuery()
                .eq(DmThingEvent::getProductId, productId)
                .list().stream().map(e -> {
                    ThingEventVO vo = new ThingEventVO();
                    BeanUtils.copyProperties(e, vo);
                    return vo;
                }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEvent(Long productId, ThingEventCreateDTO dto) {
        checkProductExists(productId);
        DmThingEvent entity = new DmThingEvent();
        BeanUtils.copyProperties(dto, entity);
        entity.setProductId(productId);
        eventService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEvent(Long productId, Long id, ThingEventUpdateDTO dto) {
        DmThingEvent existing = eventService.lambdaQuery()
                .eq(DmThingEvent::getProductId, productId)
                .eq(DmThingEvent::getId, id).one();
        if (existing == null) throw new ServiceException(404, "事件不存在");
        BeanUtils.copyProperties(dto, existing, "id", "productId");
        eventService.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(Long productId, Long id) {
        DmThingEvent existing = eventService.lambdaQuery()
                .eq(DmThingEvent::getProductId, productId)
                .eq(DmThingEvent::getId, id).one();
        if (existing == null) throw new ServiceException(404, "事件不存在");
        eventService.removeById(id);
    }

    private ThingAttributeVO toAttrVO(DmThingAttribute entity) {
        ThingAttributeVO vo = new ThingAttributeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private ThingServiceParamVO toParamVO(DmThingServiceParam entity) {
        ThingServiceParamVO vo = new ThingServiceParamVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
