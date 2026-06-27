package com.devicemind.core.controller;

import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.IDeviceShadowBusiness;
import com.devicemind.core.model.dto.ShadowUpdateDTO;
import com.devicemind.core.model.vo.ShadowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/device-mind/shadows")
@Tag(name = "设备影子", description = "查询和更新设备影子的上报/期望状态")
public class DeviceShadowController {

    private final IDeviceShadowBusiness shadowBusiness;

    @GetMapping
    @Operation(summary = "查询设备影子")
    public Result<ShadowVO> getShadow(@RequestParam String deviceId) {
        return Result.ok(shadowBusiness.getShadow(deviceId));
    }

    @PutMapping
    @Operation(summary = "更新期望状态")
    public Result<Void> updateDesired(@RequestParam String deviceId, @Valid @RequestBody ShadowUpdateDTO dto) {
        shadowBusiness.updateDesired(deviceId, dto);
        return Result.ok();
    }
}
