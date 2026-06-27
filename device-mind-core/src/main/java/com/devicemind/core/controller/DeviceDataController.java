package com.devicemind.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.IDeviceDataBusiness;
import com.devicemind.core.model.dto.DeviceDataQueryDTO;
import com.devicemind.core.model.dto.DeviceDataRequest;
import com.devicemind.core.model.vo.DeviceDataVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/device-mind/device-data")
@Tag(name = "设备数据", description = "设备数据上报与查询")
public class DeviceDataController {

    private final IDeviceDataBusiness business;

    @PostMapping
    @Operation(summary = "接收设备上报数据")
    public Result<Void> saveDeviceData(@Valid @RequestBody DeviceDataRequest request) {
        business.processDeviceData(request);
        return Result.ok();
    }

    @PostMapping("/list")
    @Operation(summary = "查询设备时序数据")
    public Result<Page<DeviceDataVO>> queryData(@Valid @RequestBody DeviceDataQueryDTO query) {
        return Result.ok(business.queryData(query));
    }
}
