package com.devicemind.core.business.impl;

import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.IDeviceShadowBusiness;
import com.devicemind.core.model.dto.ShadowUpdateDTO;
import com.devicemind.core.model.entity.DmDeviceShadow;
import com.devicemind.core.model.vo.ShadowVO;
import com.devicemind.core.stdsvc.intf.IDmDeviceShadowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceShadowBusiness implements IDeviceShadowBusiness {

    private final IDmDeviceShadowService shadowService;
    private final ObjectMapper objectMapper;

    @Override
    public ShadowVO getShadow(String deviceId) {
        DmDeviceShadow shadow = shadowService.getById(deviceId);
        if (shadow == null) {
            ShadowVO vo = new ShadowVO();
            vo.setDeviceId(deviceId);
            return vo;
        }
        ShadowVO vo = new ShadowVO();
        vo.setDeviceId(shadow.getDeviceId());
        vo.setReported(parseJson(shadow.getReported()));
        vo.setDesired(parseJson(shadow.getDesired()));
        vo.setReportedVersion(shadow.getReportedVersion());
        vo.setDesiredVersion(shadow.getDesiredVersion());
        vo.setUpdatedDate(shadow.getUpdatedDate());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDesired(String deviceId, ShadowUpdateDTO dto) {
        DmDeviceShadow existing = shadowService.getById(deviceId);
        int version = existing != null && existing.getDesiredVersion() != null
                ? existing.getDesiredVersion() + 1 : 1;
        String desiredJson;
        try {
            desiredJson = objectMapper.writeValueAsString(dto.getDesired());
        } catch (JsonProcessingException e) {
            throw new ServiceException("期望状态序列化失败");
        }
        DmDeviceShadow shadow = new DmDeviceShadow()
                .setDeviceId(deviceId)
                .setDesired(desiredJson)
                .setDesiredVersion(version)
                .setUpdatedDate(new Date());
        if (existing != null) {
            shadow.setReported(existing.getReported());
            shadow.setReportedVersion(existing.getReportedVersion());
        }
        shadowService.saveOrUpdate(shadow);
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("影子 JSON 解析失败: {}", json, e);
            return null;
        }
    }
}
