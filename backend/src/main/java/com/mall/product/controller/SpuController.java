package com.mall.product.controller;

import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.product.dto.SpuQuery;
import com.mall.product.service.SpuService;
import com.mall.product.vo.SpuDetailVO;
import com.mall.product.vo.SpuListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品 - SPU")
@RestController
@RequestMapping("/api/product/spus")
@RequiredArgsConstructor
public class SpuController {

    private final SpuService spuService;

    @Operation(summary = "SPU 分页列表",
            description = "只返回已上架且审核通过的商品；支持关键字（名称/副标题/品牌）、分类（含子分类）、排序")
    @GetMapping
    public R<PageResult<SpuListItemVO>> page(@Valid SpuQuery query) {
        return R.ok(spuService.page(query));
    }

    @Operation(summary = "SPU 详情", description = "含轮播图/详情图与全部启用 SKU（规格 + 可售库存）")
    @GetMapping("/{id}")
    public R<SpuDetailVO> detail(@Parameter(description = "SPU ID") @PathVariable("id") Long id) {
        return R.ok(spuService.detail(id));
    }
}
