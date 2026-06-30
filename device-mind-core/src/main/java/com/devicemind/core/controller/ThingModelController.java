package com.devicemind.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.IThingModelBusiness;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/things")
@Tag(name = "物模型管理", description = "产品下的属性/服务/事件定义")
public class ThingModelController {

    @Autowired
    private IThingModelBusiness thingModelBusiness;

    @GetMapping("/attributes")
    @Operation(summary = "查询属性列表")
    public Result<List<ThingAttributeVO>> listAttributes(@RequestParam Long productId) {
        return Result.ok(thingModelBusiness.listAttributes(productId));
    }

    @PostMapping("/attributes")
    @Operation(summary = "新增属性")
    public Result<Void> createAttribute(@RequestParam Long productId, @Valid @RequestBody ThingAttributeCreateDTO dto) {
        thingModelBusiness.createAttribute(productId, dto);
        return Result.ok();
    }

    @PutMapping("/attributes")
    @Operation(summary = "更新属性")
    public Result<Void> updateAttribute(@RequestParam Long productId, @RequestParam Long id,
                                        @Valid @RequestBody ThingAttributeUpdateDTO dto) {
        thingModelBusiness.updateAttribute(productId, id, dto);
        return Result.ok();
    }

    @DeleteMapping("/attributes")
    @Operation(summary = "删除属性")
    public Result<Void> deleteAttribute(@RequestParam Long productId, @RequestParam Long id) {
        thingModelBusiness.deleteAttribute(productId, id);
        return Result.ok();
    }

    @GetMapping("/services")
    @Operation(summary = "查询服务列表（含参数）")
    public Result<List<ThingServiceVO>> listServices(@RequestParam Long productId) {
        return Result.ok(thingModelBusiness.listServices(productId));
    }

    @PostMapping("/services")
    @Operation(summary = "新增服务（含参数）")
    public Result<Void> createService(@RequestParam Long productId, @Valid @RequestBody ThingServiceCreateDTO dto) {
        thingModelBusiness.createService(productId, dto);
        return Result.ok();
    }

    @PutMapping("/services")
    @Operation(summary = "更新服务")
    public Result<Void> updateService(@RequestParam Long productId, @RequestParam Long id,
                                      @Valid @RequestBody ThingServiceUpdateDTO dto) {
        thingModelBusiness.updateService(productId, id, dto);
        return Result.ok();
    }

    @DeleteMapping("/services")
    @Operation(summary = "删除服务")
    public Result<Void> deleteService(@RequestParam Long productId, @RequestParam Long id) {
        thingModelBusiness.deleteService(productId, id);
        return Result.ok();
    }

    @GetMapping("/events")
    @Operation(summary = "查询事件列表")
    public Result<List<ThingEventVO>> listEvents(@RequestParam Long productId) {
        return Result.ok(thingModelBusiness.listEvents(productId));
    }

    @PostMapping("/events")
    @Operation(summary = "新增事件")
    public Result<Void> createEvent(@RequestParam Long productId, @Valid @RequestBody ThingEventCreateDTO dto) {
        thingModelBusiness.createEvent(productId, dto);
        return Result.ok();
    }

    @PutMapping("/events")
    @Operation(summary = "更新事件")
    public Result<Void> updateEvent(@RequestParam Long productId, @RequestParam Long id,
                                    @Valid @RequestBody ThingEventUpdateDTO dto) {
        thingModelBusiness.updateEvent(productId, id, dto);
        return Result.ok();
    }

    @DeleteMapping("/events")
    @Operation(summary = "删除事件")
    public Result<Void> deleteEvent(@RequestParam Long productId, @RequestParam Long id) {
        thingModelBusiness.deleteEvent(productId, id);
        return Result.ok();
    }
}
