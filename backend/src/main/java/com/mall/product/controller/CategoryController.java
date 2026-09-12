package com.mall.product.controller;

import com.mall.common.R;
import com.mall.product.service.CategoryService;
import com.mall.product.vo.CategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品 - 分类")
@RestController
@RequestMapping("/api/product/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树", description = "返回启用状态的分类树（最多三级），游客可访问")
    @GetMapping("/tree")
    public R<List<CategoryTreeVO>> tree() {
        return R.ok(categoryService.tree());
    }
}
