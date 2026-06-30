package com.devicemind.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.ICommandLogBusiness;
import com.devicemind.core.model.dto.CommandLogPageQueryDTO;
import com.devicemind.core.model.vo.CommandLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/command-logs")
@Tag(name = "指令日志", description = "指令下发记录查询")
public class CommandLogController {

    @Autowired
    private ICommandLogBusiness commandLogBusiness;

    @PostMapping("/list")
    @Operation(summary = "分页查询指令日志")
    public Result<Page<CommandLogVO>> list(@Valid @RequestBody CommandLogPageQueryDTO query) {
        return Result.ok(commandLogBusiness.listPage(query));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询指令日志详情")
    public Result<CommandLogVO> getById(@RequestParam Long id) {
        return Result.ok(commandLogBusiness.getById(id));
    }
}
