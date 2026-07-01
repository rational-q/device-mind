package com.devicemind.core.business.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.enums.DeviceStatus;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.common.kafka.producer.DeviceLifecycleProducer;
import com.devicemind.core.business.intf.IDeviceBusiness;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.model.entity.DmProductDO;
import com.devicemind.core.model.vo.DeviceVO;
import com.devicemind.core.persistence.dao.mysql.DeviceDao;
import com.devicemind.core.stdsvc.intf.IDmDeviceService;
import com.devicemind.core.stdsvc.intf.IDmProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class DeviceBusiness implements IDeviceBusiness {

    @Autowired
    private IDmDeviceService deviceService;
    @Autowired
    private IDmProductService productService;
        @Autowired
    private DeviceDao deviceDao;
    @Autowired
    private DeviceLifecycleProducer lifecycleProducer;
    @Autowired
    private com.devicemind.core.support.CommandRetrySupport commandRetryService;
    @Autowired
    private com.devicemind.core.support.SmsSupport smsService;

    @Value("${device.offline.alert-phone:}")
    private String offlineAlertPhone;

    @Override
    public Page<DeviceVO> listPage(DevicePageQueryDTO query) {
        Page<DeviceVO> page = Page.of(query.getPageNum(), query.getPageSize());
        IPage<DeviceVO> result = deviceDao.selectDevicePage(
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
        entity.setStatus(DeviceStatus.OFFLINE.name());
        deviceService.save(entity);

        // 通知 Broker 注册设备（Kafka 同步发送，确保生命周期事件不丢）
        lifecycleProducer.sendSync(
                com.devicemind.common.kafka.model.DeviceLifecycleEvent.register(dto.getDeviceId()),
                java.time.Duration.ofSeconds(10));
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
        DmDevice device = deviceService.getById(id);
        if (device == null) {
            throw new ServiceException(404, "设备不存在");
        }
        deviceService.removeById(id);

        // 通知 Broker 注销设备（Kafka 同步发送，确保生命周期事件不丢）
        lifecycleProducer.sendSync(
                com.devicemind.common.kafka.model.DeviceLifecycleEvent.unregister(device.getDeviceId()),
                java.time.Duration.ofSeconds(10));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatusByDeviceId(String deviceId, String status) {
        DmDevice device = deviceService.lambdaQuery()
                .eq(DmDevice::getDeviceId, deviceId)
                .one();
        if (device == null) {
            log.warn("设备不存在，无法更新状态: deviceId={}", deviceId);
            return;
        }
        DmDevice update = new DmDevice();
        update.setId(device.getId());
        update.setStatus(status);
        if (DeviceStatus.isOnline(status)) {
            update.setLastOnlineTime(new Date());
        }
        deviceService.updateById(update);
        log.info("设备状态更新: deviceId={}, old={}→new={}", deviceId, device.getStatus(), status);

        // 设备上线时投递待发送指令
        if (DeviceStatus.isOnline(status)) {
            commandRetryService.deliverPendingCommands(deviceId);
        }

        // 设备无故掉线 → 短信通知
        if (DeviceStatus.isOffline(status) && DeviceStatus.isOnline(device.getStatus())) {
            if (offlineAlertPhone != null && !offlineAlertPhone.isBlank()) {
                smsService.send(List.of(offlineAlertPhone),
                        "【DeviceMind】设备离线告警: " + deviceId
                                + " (" + (device.getName() != null ? device.getName() : "") + ")"
                                + " 于 " + new Date() + " 断开连接");
            }
        }
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
        if (DeviceStatus.isOnline(dto.getStatus())) {
            update.setLastOnlineTime(new Date());
        }
        deviceService.updateById(update);
    }
}
