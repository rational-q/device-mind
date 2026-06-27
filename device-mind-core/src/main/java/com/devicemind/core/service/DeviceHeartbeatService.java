package com.devicemind.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.model.entity.DmDeviceData;
import com.devicemind.core.persistence.mapper.mysql.DmDeviceMapper;
import com.devicemind.core.persistence.mapper.timescale.DmDeviceDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.time.ZoneId;
import java.util.List;

/**
 * 设备数据级心跳检测
 * <p>
 * 定时检查 ONLINE 设备是否长时间未上报数据，自动标记 OFFLINE。
 * 解决设备 MQTT 连接还在但不报数的问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceHeartbeatService {

    private final DmDeviceMapper deviceMapper;
    private final DmDeviceDataMapper deviceDataMapper;
    private final SmsService smsService;

    /** 无数据上报视为离线的阈值（分钟） */
    @Value("${device.heartbeat.timeout-minutes:30}")
    private int timeoutMinutes;

    /** 离线告警通知手机号 */
    @Value("${device.offline.alert-phone:}")
    private String offlineAlertPhone;

    /**
     * 每5分钟检查一次
     */
    @Scheduled(fixedDelay = 300000)
    public void checkDeviceHeartbeat() {
        // 查询所有 ONLINE 设备
        List<DmDevice> onlineDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<DmDevice>()
                        .eq(DmDevice::getStatus, "ONLINE"));

        if (onlineDevices.isEmpty()) return;

        Instant threshold = Instant.now().minusSeconds(timeoutMinutes * 60L);
        int offlineCount = 0;

        for (DmDevice device : onlineDevices) {
            // 查询该设备最近一条数据的时间
            DmDeviceData latest = deviceDataMapper.selectOne(
                    new LambdaQueryWrapper<DmDeviceData>()
                            .eq(DmDeviceData::getDeviceId, device.getDeviceId())
                            .orderByDesc(DmDeviceData::getTime)
                            .last("LIMIT 1"));

            Instant lastTime;
            if (latest != null && latest.getTime() != null) {
                lastTime = latest.getTime();
            } else if (device.getLastOnlineTime() != null) {
                // 没有数据记录，用 lastOnlineTime
                lastTime = device.getLastOnlineTime().toInstant();
            } else {
                continue; // 无任何时间参考，跳过
            }

            if (lastTime.isBefore(threshold)) {
                // 标记离线
                DmDevice update = new DmDevice();
                update.setId(device.getId());
                update.setStatus("OFFLINE");
                deviceMapper.updateById(update);

                log.warn("数据心跳超时，设备标记离线: deviceId={}, lastData={}, threshold={}",
                        device.getDeviceId(), lastTime, threshold);
                offlineCount++;

                // 短信通知
                if (offlineAlertPhone != null && !offlineAlertPhone.isBlank()) {
                    smsService.send(List.of(offlineAlertPhone),
                            "【DeviceMind】设备数据离线告警: " + device.getDeviceId()
                                    + " (" + (device.getName() != null ? device.getName() : "") + ")"
                                    + " 已超过" + timeoutMinutes + "分钟未上报数据");
                }
            }
        }

        if (offlineCount > 0) {
            log.info("数据心跳检测完成: {} 台设备标记离线", offlineCount);
        }
    }
}
