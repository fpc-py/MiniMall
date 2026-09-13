package com.mall.product.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.RequirePermission;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.entity.SpuImage;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuImageMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.vo.SkuVO;
import com.mall.product.vo.SpuDetailVO;
import com.mall.product.vo.SpuImageVO;
import com.mall.product.vo.SpuListItemVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "平台端 - SPU商品管理")
@RestController
@RequestMapping("/api/admin/spus")
@RequiredArgsConstructor
public class AdminSpuController {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final SpuImageMapper spuImageMapper;

    @Data
    public static class SpuCreateRequest {
        @NotBlank(message = "商品名称不能为空")
        private String name;
        private String subtitle;
        private Long categoryId;
        private String brand;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Integer sort;
        private String description;
        private List<String> images;
        private List<SkuRequest> skus;
    }

    @Data
    public static class SpuUpdateRequest {
        @NotNull(message = "商品ID不能为空")
        private Long id;
        private String name;
        private String subtitle;
        private Long categoryId;
        private String brand;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Integer sort;
        private Integer status;
        private Integer auditStatus;
        private String description;
        private List<String> images;
    }

    @Data
    public static class SkuRequest {
        private String spec;
        private BigDecimal price;
        private Integer stock;
        private String skuCode;
        private Integer sort;
    }

    @Operation(summary = "SPU 列表（管理用）", description = "返回全部 SPU，含下架/未审核")
    @GetMapping
    @RequirePermission("admin:spu:list")
    public R<PageResult<SpuListItemVO>> list(@Valid PageQuery pageQuery,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long categoryId) {
        Page<Spu> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        var wrapper = Wrappers.<Spu>lambdaQuery()
                .like(keyword != null, Spu::getName, keyword)
                .eq(categoryId != null, Spu::getCategoryId, categoryId)
                .orderByDesc(Spu::getCreateTime)
                .orderByDesc(Spu::getId);
        var result = spuMapper.selectPage(page, wrapper);
        var vos = result.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos));
    }

    @Operation(summary = "SPU 详情（管理用）")
    @GetMapping("/{id}")
    @RequirePermission("admin:spu:detail")
    public R<SpuDetailVO> detail(@PathVariable("id") Long id) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) {
            return R.fail(400, "商品不存在");
        }
        SpuDetailVO vo = new SpuDetailVO();
        vo.setId(spu.getId());
        vo.setName(spu.getName());
        vo.setSubtitle(spu.getSubtitle());
        vo.setCategoryId(spu.getCategoryId());
        vo.setBrand(spu.getBrand());
        vo.setDescription(spu.getDescription());
        vo.setMinPrice(spu.getMinPrice());
        vo.setMaxPrice(spu.getMaxPrice());
        vo.setTotalSales(spu.getTotalSales());
        // images
        var images = spuImageMapper.selectList(Wrappers.<SpuImage>lambdaQuery()
                .eq(SpuImage::getSpuId, id).orderByAsc(SpuImage::getSort));
        vo.setImages(images.stream().map(img -> {
            var imgVo = new SpuImageVO();
            imgVo.setId(img.getId());
            imgVo.setUrl(img.getUrl());
            imgVo.setType(img.getType());
            imgVo.setSort(img.getSort());
            return imgVo;
        }).collect(Collectors.toList()));
        // skus
        var skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                .eq(Sku::getSpuId, id).orderByAsc(Sku::getSort));
        vo.setSkus(skus.stream().map(sku -> {
            var skuVo = new SkuVO();
            skuVo.setId(sku.getId());
            skuVo.setSpuId(sku.getSpuId());
            skuVo.setSkuCode(sku.getSkuCode());
            skuVo.setPrice(sku.getPrice());
            skuVo.setStock(sku.getStock());
            return skuVo;
        }).collect(Collectors.toList()));
        return R.ok(vo);
    }

    @Operation(summary = "创建 SPU")
    @PostMapping
    @RequirePermission("admin:spu:create")
    public R<Void> create(@Valid @RequestBody SpuCreateRequest req) {
        Spu spu = new Spu();
        spu.setName(req.getName());
        spu.setSubtitle(req.getSubtitle());
        spu.setCategoryId(req.getCategoryId());
        spu.setBrand(req.getBrand());
        spu.setMinPrice(req.getMinPrice());
        spu.setMaxPrice(req.getMaxPrice());
        spu.setDescription(req.getDescription());
        spu.setStatus(0);
        spu.setAuditStatus(1);
        spu.setTotalSales(0);
        spuMapper.insert(spu);
        saveImages(spu.getId(), req.getImages());
        if (req.getSkus() != null) {
            for (SkuRequest skuReq : req.getSkus()) {
                Sku sku = new Sku();
                sku.setSpuId(spu.getId());
                sku.setSkuCode(skuReq.getSkuCode());
                sku.setPrice(skuReq.getPrice());
                sku.setStock(skuReq.getStock());
                sku.setStatus(1);
                skuMapper.insert(sku);
            }
        }
        return R.ok();
    }

    @Operation(summary = "更新 SPU")
    @PutMapping
    @RequirePermission("admin:spu:update")
    public R<Void> update(@Valid @RequestBody SpuUpdateRequest req) {
        Spu spu = new Spu();
        spu.setId(req.getId());
        spu.setName(req.getName());
        spu.setSubtitle(req.getSubtitle());
        spu.setCategoryId(req.getCategoryId());
        spu.setBrand(req.getBrand());
        spu.setMinPrice(req.getMinPrice());
        spu.setMaxPrice(req.getMaxPrice());
        spu.setDescription(req.getDescription());
        spuMapper.updateById(spu);
        if (req.getImages() != null) {
            spuImageMapper.delete(Wrappers.<SpuImage>lambdaQuery().eq(SpuImage::getSpuId, req.getId()));
            saveImages(req.getId(), req.getImages());
        }
        return R.ok();
    }

    @Operation(summary = "删除 SPU")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:spu:delete")
    public R<Void> delete(@PathVariable("id") Long id) {
        spuMapper.deleteById(id);
        spuImageMapper.delete(Wrappers.<SpuImage>lambdaQuery().eq(SpuImage::getSpuId, id));
        skuMapper.delete(Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, id));
        return R.ok();
    }

    private void saveImages(Long spuId, List<String> urls) {
        if (urls == null) return;
        for (int i = 0; i < urls.size(); i++) {
            SpuImage img = new SpuImage();
            img.setSpuId(spuId);
            img.setUrl(urls.get(i));
            img.setType(0);
            img.setSort(i);
            spuImageMapper.insert(img);
        }
    }

    private SpuListItemVO toListItem(Spu spu) {
        SpuListItemVO vo = new SpuListItemVO();
        vo.setId(spu.getId());
        vo.setName(spu.getName());
        vo.setSubtitle(spu.getSubtitle());
        vo.setBrand(spu.getBrand());
        vo.setMinPrice(spu.getMinPrice());
        vo.setMaxPrice(spu.getMaxPrice());
        vo.setTotalSales(spu.getTotalSales());
        return vo;
    }
}
