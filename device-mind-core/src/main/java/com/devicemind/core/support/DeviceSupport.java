package com.devicemind.core.support;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.model.entity.DmProductDO;
import com.devicemind.core.stdsvc.intf.IDmDeviceService;
import com.devicemind.core.stdsvc.intf.IDmProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备业务服务
 * <p>
 * 封装设备相关的跨表查询业务逻辑
 */
@Slf4j
@Service
public class DeviceSupport {

    @Autowired
    private IDmDeviceService dmDeviceSupport;
    @Autowired
    private IDmProductService dmProductService;

    /**
     * 根据 deviceId 查询对应的 productKey
     * <p>
     * 查 dm_device → 获取 productId → 查 dm_product → 返回 productKey
     *
     * @param deviceId 设备唯一标识
     * @return productKey，设备或产品不存在时返回 null
     */
    public String getProductKeyByDeviceId(String deviceId) {
        DmDevice device = dmDeviceSupport.getOne(
                new LambdaQueryWrapper<DmDevice>()
                        .eq(DmDevice::getDeviceId, deviceId)
        );
        if (device == null) {
            log.warn("设备不存在: deviceId={}", deviceId);
            return null;
        }

        DmProductDO product = dmProductService.getById(device.getProductId());
        if (product == null) {
            log.warn("产品不存在: productId={}, deviceId={}", device.getProductId(), deviceId);
            return null;
        }

        log.debug("查询 productKey: deviceId={} → productKey={}", deviceId, product.getProductKey());
        return product.getProductKey();
    }
}
