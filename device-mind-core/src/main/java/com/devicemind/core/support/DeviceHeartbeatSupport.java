package com.devicemind.core.support;

import com.devicemind.core.stdsvc.intf.IDmDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.common.enums.DeviceStatus;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.persistence.dao.timescale.DeviceDataDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备数据级心跳检测
 * <p>
 * 定时检查 ONLINE 设备是否长时间未上报数据，自动标记 OFFLINE。
 * 解决设备 MQTT 连接还在但不报数的问题。
 * <p>
 * 使用批量查询替代逐条 N+1，一次查出所有在线设备的最新数据时间。
 */
@Slf4j
@Service
public class DeviceHeartbeatSupport {

    @Autowired
    private IDmDeviceService dmDeviceSupport;
    @Autowired
    private DeviceDataDao deviceDataDao;
    @Autowired
    private SmsSupport smsService;

    @Value("${device.heartbeat.timeout-minutes:30}")
    private int timeoutMinutes;

    @Value("${device.offline.alert-phone:}")
    private String offlineAlertPhone;

    /**
     * 每5分钟检查一次
     */
    @Scheduled(fixedDelay = 300000)
    public void checkDeviceHeartbeat() {
        List<DmDevice> onlineDevices = dmDeviceSupport.lambdaQuery()
                .eq(DmDevice::getStatus, DeviceStatus.ONLINE.name())
                .list();

        if (onlineDevices.isEmpty()) return;

        // 批量查询所有在线设备的最新数据时间（一次查询代替 N 次）
        List<String> deviceIds = onlineDevices.stream()
                .map(DmDevice::getDeviceId)
                .collect(Collectors.toList());
        Map<String, Instant> latestTimeMap = deviceDataDao.selectLatestDataTimes(deviceIds)
                .stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("deviceId"),
                        m -> {
                            Object t = m.get("lastTime");
                            if (t instanceof java.sql.Timestamp ts) return ts.toInstant();
                            if (t instanceof Instant inst) return inst;
                            return null;
                        },
                        (a, b) -> a));

        Instant threshold = Instant.now().minusSeconds(timeoutMinutes * 60L);
        int offlineCount = 0;

        for (DmDevice device : onlineDevices) {
            Instant lastTime = latestTimeMap.get(device.getDeviceId());
            if (lastTime == null && device.getLastOnlineTime() != null) {
                lastTime = device.getLastOnlineTime().toInstant();
            }
            if (lastTime == null) continue;

            if (lastTime.isBefore(threshold)) {
                DmDevice update = new DmDevice();
                update.setId(device.getId());
                update.setStatus(DeviceStatus.OFFLINE.name());
                dmDeviceSupport.updateById(update);

                log.warn("数据心跳超时，设备标记离线: deviceId={}, lastData={}, threshold={}",
                        device.getDeviceId(), lastTime, threshold);
                offlineCount++;

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
