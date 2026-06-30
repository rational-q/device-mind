package com.devicemind.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.IAlertBusiness;
import com.devicemind.core.model.dto.AlertPageQueryDTO;
import com.devicemind.core.model.vo.AlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/alerts")
@Tag(name = "告警管理", description = "告警事件查询与处理")
public class AlertController {

    @Autowired
    private IAlertBusiness alertBusiness;

    @PostMapping("/list")
    @Operation(summary = "分页查询告警列表")
    public Result<Page<AlertVO>> list(@Valid @RequestBody AlertPageQueryDTO query) {
        return Result.ok(alertBusiness.listPage(query));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询告警详情")
    public Result<AlertVO> getById(@RequestParam Long id) {
        return Result.ok(alertBusiness.getById(id));
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认告警")
    public Result<Void> confirm(@RequestParam Long id) {
        alertBusiness.confirm(id);
        return Result.ok();
    }

    @PutMapping("/resolve")
    @Operation(summary = "恢复告警")
    public Result<Void> resolve(@RequestParam Long id) {
        alertBusiness.resolve(id);
        return Result.ok();
    }
}
