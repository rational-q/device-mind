package com.devicemind.core.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.IDeviceBusiness;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.model.entity.DmProductDO;
import com.devicemind.core.model.vo.DeviceVO;
import com.devicemind.core.persistence.mapper.mysql.DmDeviceMapper;
import com.devicemind.core.stdsvc.intf.IDmDeviceService;
import com.devicemind.core.stdsvc.intf.IDmProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceBusiness implements IDeviceBusiness {

    private final IDmDeviceService deviceService;
    private final IDmProductService productService;
    private final DmDeviceMapper deviceMapper;

    @Override
    public Page<DeviceVO> listPage(DevicePageQueryDTO query) {
        Page<DeviceVO> page = Page.of(query.getPageNum(), query.getPageSize());
        IPage<DeviceVO> result = deviceMapper.selectDevicePage(
                page, query.getDeviceId(), query.getProductId(), query.getStatus());
        // 如果返回的产品名称为空（LEFT JOIN 无匹配），兜底填充未知
        result.getRecords().forEach(vo -> {
            if (vo.getProductName() == null) {
                vo.setProductName("未知产品");
            }
        });
        return (Page<DeviceVO>) result;
    }

    @Override
    public DeviceVO getById(Long id) {
        DmDevice device = deviceService.getById(id);
        if (device == null) {
            throw new ServiceException(404, "设备不存在");
        }
        DeviceVO vo = new DeviceVO();
        BeanUtils.copyProperties(device, vo);
        // 填充产品名称
        DmProductDO product = productService.getById(device.getProductId());
        vo.setProductName(product != null ? product.getName() : "未知产品");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DeviceCreateDTO dto) {
        // 校验产品存在
        if (productService.getById(dto.getProductId()) == null) {
            throw new ServiceException("产品不存在");
        }
        // 校验 deviceId 唯一
        long count = deviceService.lambdaQuery()
                .eq(DmDevice::getDeviceId, dto.getDeviceId())
                .count();
        if (count > 0) {
            throw new ServiceException("设备ID已存在: " + dto.getDeviceId());
        }
        DmDevice entity = new DmDevice();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus("OFFLINE");
        deviceService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DeviceUpdateDTO dto) {
        DmDevice existing = deviceService.getById(id);
        if (existing == null) {
            throw new ServiceException(404, "设备不存在");
        }
        BeanUtils.copyProperties(dto, existing, "id", "deviceId", "productId", "status");
        deviceService.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (deviceService.getById(id) == null) {
            throw new ServiceException(404, "设备不存在");
        }
        deviceService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, DeviceStatusUpdateDTO dto) {
        DmDevice existing = deviceService.getById(id);
        if (existing == null) {
            throw new ServiceException(404, "设备不存在");
        }
        DmDevice update = new DmDevice();
        update.setId(id);
        update.setStatus(dto.getStatus());
        if ("ONLINE".equals(dto.getStatus())) {
            update.setLastOnlineTime(new Date());
        }
        deviceService.updateById(update);
    }
}
