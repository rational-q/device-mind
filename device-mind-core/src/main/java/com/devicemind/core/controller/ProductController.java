package com.devicemind.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.utils.Result;
import com.devicemind.core.business.intf.IProductBusiness;
import com.devicemind.core.model.dto.ProductCreateDTO;
import com.devicemind.core.model.dto.ProductPageQueryDTO;
import com.devicemind.core.model.dto.ProductUpdateDTO;
import com.devicemind.core.model.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/device-mind/products")
@Tag(name = "产品管理", description = "产品 CRUD")
public class ProductController {

    private final IProductBusiness productBusiness;

    @PostMapping("/list")
    @Operation(summary = "分页查询产品列表")
    public Result<Page<ProductVO>> list(@Valid @RequestBody ProductPageQueryDTO query) {
        return Result.ok(productBusiness.listPage(query));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询产品详情")
    public Result<ProductVO> getById(@RequestParam Long id) {
        return Result.ok(productBusiness.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增产品")
    public Result<Void> create(@Valid @RequestBody ProductCreateDTO dto) {
        productBusiness.create(dto);
        return Result.ok();
    }

    @PutMapping
    @Operation(summary = "更新产品")
    public Result<Void> update(@RequestParam Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        productBusiness.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除产品")
    public Result<Void> delete(@RequestParam Long id) {
        productBusiness.delete(id);
        return Result.ok();
    }
}
