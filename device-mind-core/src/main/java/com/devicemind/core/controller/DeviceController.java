package com.devicemind.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.IDeviceBusiness;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.vo.DeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/devices")
@Tag(name = "设备管理", description = "设备注册、查询、状态管理")
public class DeviceController {

    @Autowired
    private IDeviceBusiness deviceBusiness;

    @PostMapping("/list")
    @Operation(summary = "分页查询设备列表")
    public Result<Page<DeviceVO>> list(@Valid @RequestBody DevicePageQueryDTO query) {
        return Result.ok(deviceBusiness.listPage(query));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询设备详情")
    public Result<DeviceVO> getById(@RequestParam Long id) {
        return Result.ok(deviceBusiness.getById(id));
    }

    @PostMapping
    @Operation(summary = "注册设备")
    public Result<Void> create(@Valid @RequestBody DeviceCreateDTO dto) {
        deviceBusiness.create(dto);
        return Result.ok();
    }

    @PutMapping
    @Operation(summary = "更新设备信息")
    public Result<Void> update(@RequestParam Long id, @Valid @RequestBody DeviceUpdateDTO dto) {
        deviceBusiness.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除设备")
    public Result<Void> delete(@RequestParam Long id) {
        deviceBusiness.delete(id);
        return Result.ok();
    }

    @PutMapping("/status")
    @Operation(summary = "更新设备状态（按内部ID）")
    public Result<Void> updateStatus(@RequestParam Long id, @Valid @RequestBody DeviceStatusUpdateDTO dto) {
        deviceBusiness.updateStatus(id, dto);
        return Result.ok();
    }
}
