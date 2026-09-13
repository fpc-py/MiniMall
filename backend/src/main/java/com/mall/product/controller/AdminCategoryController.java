package com.mall.product.controller;

import com.mall.common.R;
import com.mall.common.security.RequirePermission;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "平台端 - 类目管理")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryMapper categoryMapper;

    @Data
    public static class CategoryCreateRequest {
        @NotBlank(message = "分类名称不能为空")
        private String name;
        private Long parentId = 0L;
        private Integer level;
        private String icon;
        private String image;
        private Integer sort = 0;
        private Integer status = 1;
    }

    @Data
    public static class CategoryUpdateRequest {
        @NotNull(message = "分类ID不能为空")
        private Long id;
        private String name;
        private Long parentId;
        private Integer level;
        private String icon;
        private String image;
        private Integer sort;
        private Integer status;
    }

    @Operation(summary = "获取全部分类（管理用）", description = "返回所有分类，含隐藏")
    @GetMapping
    @RequirePermission("admin:category:list")
    public R<List<Category>> list() {
        return R.ok(categoryMapper.selectList(null));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    @RequirePermission("admin:category:create")
    public R<Void> create(@Valid @RequestBody CategoryCreateRequest req) {
        Category category = new Category();
        category.setName(req.getName());
        category.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        category.setLevel(req.getLevel());
        category.setIcon(req.getIcon());
        category.setImage(req.getImage());
        category.setSort(req.getSort() == null ? 0 : req.getSort());
        category.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        categoryMapper.insert(category);
        return R.ok();
    }

    @Operation(summary = "更新分类")
    @PutMapping
    @RequirePermission("admin:category:update")
    public R<Void> update(@Valid @RequestBody CategoryUpdateRequest req) {
        Category category = new Category();
        category.setId(req.getId());
        category.setName(req.getName());
        category.setParentId(req.getParentId());
        category.setLevel(req.getLevel());
        category.setIcon(req.getIcon());
        category.setImage(req.getImage());
        category.setSort(req.getSort());
        category.setStatus(req.getStatus());
        categoryMapper.updateById(category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:category:delete")
    public R<Void> delete(@PathVariable("id") Long id) {
        categoryMapper.deleteById(id);
        return R.ok();
    }
}
