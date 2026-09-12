package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageResult;
import com.mall.common.util.SqlUtils;
import com.mall.product.converter.ProductConverter;
import com.mall.product.dto.SpuQuery;
import com.mall.product.entity.Category;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 商品 SPU 查询服务：只对外暴露「已上架 + 审核通过」的商品。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpuService {

    /** 上架 */
    public static final int STATUS_ON_SALE = 1;
    /** 审核通过 */
    public static final int AUDIT_PASSED = 1;
    /** SKU 启用 */
    public static final int SKU_ENABLED = 1;

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final SpuImageMapper spuImageMapper;
    private final CategoryService categoryService;
    private final ProductConverter converter;
    private final ObjectMapper objectMapper;

    /**
     * SPU 分页列表：关键字搜索 + 分类筛选（含子分类）+ 排序白名单。
     */
    public PageResult<SpuListItemVO> page(SpuQuery query) {
        SpuQuery q = query == null ? new SpuQuery() : query;
        Page<Spu> page = new Page<>(q.safePageNum(), q.safePageSize());

        LambdaQueryWrapper<Spu> wrapper = Wrappers.<Spu>lambdaQuery()
                .eq(Spu::getStatus, STATUS_ON_SALE)
                .eq(Spu::getAuditStatus, AUDIT_PASSED)
                .eq(q.getShopId() != null, Spu::getShopId, q.getShopId());

        String keyword = SqlUtils.normalizeKeyword(q.getKeyword());
        if (keyword != null) {
            // 关键字已做 LIKE 通配符转义，且以占位符绑定，不存在 SQL 注入
            wrapper.and(w -> w.like(Spu::getName, keyword)
                    .or().like(Spu::getSubtitle, keyword)
                    .or().like(Spu::getBrand, keyword));
        }
        if (q.getCategoryId() != null) {
            List<Long> categoryIds = categoryService.listSelfAndDescendantIds(q.getCategoryId());
            if (!categoryIds.isEmpty()) {
                wrapper.in(Spu::getCategoryId, categoryIds);
            }
        }
        applySort(wrapper, q.getSort());

        Page<Spu> result = spuMapper.selectPage(page, wrapper);
        return PageResult.of(result, converter::toListItem);
    }

    /**
     * SPU 详情：含图片与全部启用 SKU。
     */
    public SpuDetailVO detail(Long spuId) {
        if (spuId == null) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "商品ID不能为空");
        }
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new MallException(ErrorCode.Biz.PRODUCT_NOT_FOUND);
        }
        if (!isVisible(spu)) {
            throw new MallException(ErrorCode.Biz.PRODUCT_OFFLINE);
        }
        SpuDetailVO vo = converter.toDetail(spu);
        Category category = categoryService.findById(spu.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setImages(listImages(spuId));
        vo.setSkus(listSkus(spuId));
        return vo;
    }

    /** 商品是否对游客/买家可见 */
    public boolean isVisible(Spu spu) {
        return spu != null
                && spu.getStatus() != null && spu.getStatus() == STATUS_ON_SALE
                && spu.getAuditStatus() != null && spu.getAuditStatus() == AUDIT_PASSED;
    }

    private List<SpuImageVO> listImages(Long spuId) {
        List<SpuImage> images = spuImageMapper.selectList(Wrappers.<SpuImage>lambdaQuery()
                .eq(SpuImage::getSpuId, spuId)
                .orderByAsc(SpuImage::getSort)
                .orderByAsc(SpuImage::getId));
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        return images.stream()
                .map(image -> SpuImageVO.of(image.getId(), image.getUrl(), image.getType(), image.getSort()))
                .toList();
    }

    private List<SkuVO> listSkus(Long spuId) {
        List<Sku> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                .eq(Sku::getSpuId, spuId)
                .eq(Sku::getStatus, SKU_ENABLED)
                .orderByAsc(Sku::getSort)
                .orderByAsc(Sku::getId));
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }
        return skus.stream().map(this::toSkuVO).toList();
    }

    private SkuVO toSkuVO(Sku sku) {
        SkuVO vo = converter.toSkuVO(sku);
        vo.setStock(availableStock(sku));
        vo.setAttrs(parseAttrs(sku.getAttrs()));
        return vo;
    }

    /** 可售库存 = 库存 - 冻结库存，最小为 0 */
    static int availableStock(Sku sku) {
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        int frozen = sku.getFrozenStock() == null ? 0 : sku.getFrozenStock();
        return Math.max(stock - frozen, 0);
    }

    private Map<String, String> parseAttrs(String attrsJson) {
        if (!StringUtils.hasText(attrsJson)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> attrs = objectMapper.readValue(attrsJson, new TypeReference<Map<String, String>>() {
            });
            return attrs == null ? Collections.emptyMap() : attrs;
        } catch (JsonProcessingException e) {
            log.warn("SKU 规格属性 JSON 解析失败，按空规格返回: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 排序字段白名单：非白名单值一律走默认排序，杜绝 order by 注入 */
    private void applySort(LambdaQueryWrapper<Spu> wrapper, String sort) {
        String key = sort == null ? "" : sort.trim();
        switch (key) {
            case "sales" -> wrapper.orderByDesc(Spu::getTotalSales);
            case "priceAsc" -> wrapper.orderByAsc(Spu::getMinPrice);
            case "priceDesc" -> wrapper.orderByDesc(Spu::getMinPrice);
            case "newest" -> wrapper.orderByDesc(Spu::getCreateTime);
            default -> wrapper.orderByDesc(Spu::getTotalSales);
        }
        // 追加稳定排序键，避免同值记录在翻页时重复/丢失
        wrapper.orderByDesc(Spu::getId);
    }
}
