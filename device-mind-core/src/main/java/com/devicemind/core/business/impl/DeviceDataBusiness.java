package com.devicemind.core.business.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.IDeviceDataBusiness;
import com.devicemind.core.model.dto.DeviceDataQueryDTO;
import com.devicemind.core.model.dto.DeviceDataRequest;
import com.devicemind.core.model.vo.DeviceDataVO;
import com.devicemind.core.persistence.mapper.timescale.DmDeviceDataMapper;
import com.devicemind.core.stdsvc.impl.DmDeviceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设备数据业务实现
 * <p>
 * 负责将上报请求解析为数据点列表，委托 DmDeviceDataService 写入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataBusiness implements IDeviceDataBusiness {

    private final DmDeviceDataService deviceDataService;
    private final DmDeviceDataMapper deviceDataMapper;

    @Override
    public void processDeviceData(DeviceDataRequest request) {
        long timestamp = request.getTimestamp() != null
                ? request.getTimestamp()
                : Instant.now().getEpochSecond();

        Map<String, Object> attrs = request.getAttrs();
        List<DeviceDataPoint> points = new ArrayList<>(attrs.size());
        attrs.forEach((attrName, value) ->
                points.add(new DeviceDataPoint(request.getDeviceId(), attrName, value, timestamp)));

        deviceDataService.saveData(points);
        log.info("设备数据上报处理完成: deviceId={}, 点数={}", request.getDeviceId(), points.size());
    }

    @Override
    public Page<DeviceDataVO> queryData(DeviceDataQueryDTO query) {
        Page<DeviceDataVO> page = Page.of(query.getPageNum(), query.getPageSize());
        return (Page<DeviceDataVO>) deviceDataMapper.selectDataPage(
                page, query.getDeviceId(), query.getAttrName(), query.getStart(), query.getEnd());
    }
}
