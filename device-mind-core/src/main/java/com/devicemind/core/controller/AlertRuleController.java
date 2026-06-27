package com.devicemind.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.IAlertRuleBusiness;
import com.devicemind.core.model.dto.*;
import com.devicemind.core.model.vo.AlertRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/device-mind/alert-rules")
@Tag(name = "告警规则管理", description = "告警规则 CRUD")
public class AlertRuleController {

    private final IAlertRuleBusiness alertRuleBusiness;

    @PostMapping("/list")
    @Operation(summary = "分页查询告警规则")
    public Result<Page<AlertRuleVO>> list(@Valid @RequestBody AlertRulePageQueryDTO query) {
        return Result.ok(alertRuleBusiness.listPage(query));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询告警规则详情")
    public Result<AlertRuleVO> getById(@RequestParam Long id) {
        return Result.ok(alertRuleBusiness.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增告警规则")
    public Result<Void> create(@Valid @RequestBody AlertRuleCreateDTO dto) {
        alertRuleBusiness.create(dto);
        return Result.ok();
    }

    @PutMapping
    @Operation(summary = "更新告警规则")
    public Result<Void> update(@RequestParam Long id, @Valid @RequestBody AlertRuleUpdateDTO dto) {
        alertRuleBusiness.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除告警规则")
    public Result<Void> delete(@RequestParam Long id) {
        alertRuleBusiness.delete(id);
        return Result.ok();
    }

    @PutMapping("/toggle")
    @Operation(summary = "启用/禁用告警规则")
    public Result<Void> toggle(@RequestParam Long id) {
        alertRuleBusiness.toggle(id);
        return Result.ok();
    }
}
