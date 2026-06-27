package com.devicemind.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 短信发送服务
 * <p>
 * 当前为模拟实现，仅打印日志。对接真实 SMS 网关时替换此实现。
 */
@Slf4j
@Service
public class SmsService {

    /**
     * 发送短信
     *
     * @param phoneNumbers 手机号列表
     * @param content      短信内容
     * @return true 表示发送成功
     */
    public boolean send(List<String> phoneNumbers, String content) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            log.warn("短信发送失败: 手机号为空");
            return false;
        }
        for (String phone : phoneNumbers) {
            log.info("===== [短信] 发送至 {}: {} =====", phone, content);
        }
        // TODO: 对接真实短信网关
        return true;
    }
}
