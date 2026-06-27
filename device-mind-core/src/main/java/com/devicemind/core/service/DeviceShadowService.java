package com.devicemind.core.service;

import com.devicemind.common.dto.DeviceDataPoint;
import com.devicemind.core.model.entity.DmDeviceShadow;
import com.devicemind.core.stdsvc.intf.IDmDeviceShadowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备影子业务服务
 * <p>
 * 负责将设备上报的数据点更新到 dm_device_shadow 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceShadowService {

    private final IDmDeviceShadowService dmDeviceShadowService;
    private final ObjectMapper objectMapper;

    /**
     * 更新设备影子 — 将数据点转为 JSON，upsert dm_device_shadow 表
     * <p>
     * 同一 deviceId 的数据点合并为一个 JSON 对象写入 REPORTED 字段。
     * 使用 saveOrUpdate 实现 upsert（根据 DEVICE_ID 主键判断 INSERT 还是 UPDATE）。
     *
     * @param points 设备数据点列表
     */
    public void updateReported(List<DeviceDataPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        // 按 deviceId 分组，每组构建一个影子记录
        Map<String, List<DeviceDataPoint>> grouped = points.stream()
                .collect(Collectors.groupingBy(DeviceDataPoint::getDeviceId));

        for (Map.Entry<String, List<DeviceDataPoint>> entry : grouped.entrySet()) {
            String deviceId = entry.getKey();
            List<DeviceDataPoint> devicePoints = entry.getValue();

            // attrs → JSON（支持所有数据类型）
            Map<String, Object> attrs = new LinkedHashMap<>();
            devicePoints.forEach(p -> attrs.put(p.getAttrName(), p.getValue()));

            String reportedJson;
            try {
                reportedJson = objectMapper.writeValueAsString(attrs);
            } catch (JsonProcessingException e) {
                log.error("设备影子序列化失败: deviceId={}", deviceId, e);
                continue;
            }

            // 查询已有记录，获取当前版本号
            DmDeviceShadow existing = dmDeviceShadowService.getById(deviceId);
            int newVersion = existing != null && existing.getReportedVersion() != null
                    ? existing.getReportedVersion() + 1 : 1;

            DmDeviceShadow shadow = new DmDeviceShadow()
                    .setDeviceId(deviceId)
                    .setReported(reportedJson)
                    .setReportedVersion(newVersion)
                    .setUpdatedDate(new Date());

            dmDeviceShadowService.saveOrUpdate(shadow);
            log.debug("设备影子更新: deviceId={}, attrsCount={}, version={}", deviceId, attrs.size(), newVersion);
        }
    }
}
