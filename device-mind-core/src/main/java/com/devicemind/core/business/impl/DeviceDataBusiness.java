package com.devicemind.core.business.impl;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.kafka.model.DeviceDataPoint;
import com.devicemind.core.business.intf.IDeviceDataBusiness;
import com.devicemind.core.model.dto.DeviceDataQueryDTO;
import com.devicemind.core.model.dto.DeviceDataRequest;
import com.devicemind.core.model.vo.DeviceDataVO;
import com.devicemind.core.persistence.dao.timescale.DeviceDataDao;
import com.devicemind.core.processor.DeviceDataProcessor;
import com.devicemind.core.stdsvc.intf.IDmDeviceDataService;
import com.devicemind.core.support.DeviceSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 设备数据业务实现
 * <p>
 * REST 直报入口，与 Kafka 消费路径 (DeviceDataConsumer) 共用同一套
 * DeviceDataProcessor 管道：save → shadow → alert → scene → 广播，
 * 保证两条入口行为一致（不再绕过告警/影子/场景引擎）。
 */
@Slf4j
@Service
public class DeviceDataBusiness implements IDeviceDataBusiness {

    @Autowired
    private IDmDeviceDataService deviceDataService;
    @Autowired
    private DeviceDataDao deviceDataDao;
    @Autowired
    private DeviceSupport deviceSupport;

    private final Map<String, DeviceDataProcessor> processorMap = new ConcurrentHashMap<>();

    @Autowired
    public void setProcessors(List<DeviceDataProcessor> processors) {
        processorMap.putAll(processors.stream()
                .collect(Collectors.toMap(DeviceDataProcessor::supportedProductKey, p -> p)));
    }

    @Override
    public void processDeviceData(DeviceDataRequest request) {
        if (request == null || request.getDeviceId() == null
                || request.getAttrs() == null || request.getAttrs().isEmpty()) {
            log.warn("REST 上报请求非法（缺 deviceId 或 attrs），跳过");
            return;
        }
        long timestamp = request.getTimestamp() != null
                ? request.getTimestamp()
                : Instant.now().getEpochSecond();

        String deviceId = request.getDeviceId();
        Map<String, Object> attrs = request.getAttrs();
        List<DeviceDataPoint> points = new ArrayList<>(attrs.size());
        attrs.forEach((attrName, value) ->
                points.add(new DeviceDataPoint(deviceId, attrName, value, timestamp)));

        // 路由到对应 productKey 的 Processor，走完整管道（含告警/影子/场景/广播）
        String productKey = deviceSupport.getProductKeyByDeviceId(deviceId);
        DeviceDataProcessor processor = productKey != null ? processorMap.get(productKey) : null;
        if (processor != null) {
            processor.process(points);
            log.info("设备数据上报处理完成(完整管道): deviceId={}, productKey={}, 点数={}",
                    deviceId, productKey, points.size());
        } else {
            // 无匹配处理器时至少落库，避免数据丢失
            deviceDataService.saveData(points);
            log.warn("未找到处理器，仅落库不触发告警/场景: deviceId={}, productKey={}, 点数={}",
                    deviceId, productKey, points.size());
        }
    }

    @Override
    public Page<DeviceDataVO> queryData(DeviceDataQueryDTO query) {
        Page<DeviceDataVO> page = Page.of(query.getPageNum(), query.getPageSize());
        return (Page<DeviceDataVO>) deviceDataDao.selectDataPage(
                page, query.getDeviceId(), query.getAttrName(), query.getStart(), query.getEnd());
    }
}
