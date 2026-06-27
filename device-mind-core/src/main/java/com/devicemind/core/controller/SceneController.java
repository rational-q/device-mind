package com.devicemind.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.ISceneBusiness;
import com.devicemind.core.model.dto.SceneCreateDTO;
import com.devicemind.core.model.dto.SceneUpdateDTO;
import com.devicemind.core.model.vo.SceneLogVO;
import com.devicemind.core.model.vo.SceneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/device-mind/scenes")
@Tag(name = "场景联动", description = "场景定义与触发日志")
public class SceneController {

    private final ISceneBusiness sceneBusiness;

    @PostMapping("/list")
    @Operation(summary = "分页查询场景列表")
    public Result<Page<SceneVO>> list(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(sceneBusiness.listPage(pageNum, pageSize));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询场景详情")
    public Result<SceneVO> getById(@RequestParam Long id) {
        return Result.ok(sceneBusiness.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建场景")
    public Result<Long> create(@Valid @RequestBody SceneCreateDTO dto) {
        return Result.ok(sceneBusiness.create(dto));
    }

    @PutMapping
    @Operation(summary = "更新场景")
    public Result<Void> update(@RequestParam Long id, @RequestBody SceneUpdateDTO dto) {
        sceneBusiness.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除场景")
    public Result<Void> delete(@RequestParam Long id) {
        sceneBusiness.delete(id);
        return Result.ok();
    }

    @PutMapping("/toggle")
    @Operation(summary = "启用/禁用场景")
    public Result<Void> toggle(@RequestParam Long id) {
        sceneBusiness.toggle(id);
        return Result.ok();
    }

    @PostMapping("/log/list")
    @Operation(summary = "分页查询场景执行日志")
    public Result<Page<SceneLogVO>> listLog(@RequestParam(required = false) Long sceneId,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(sceneBusiness.listLogPage(sceneId, pageNum, pageSize));
    }
}
