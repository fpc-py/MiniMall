package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.util.SqlUtils;
import com.mall.product.converter.ProductConverter;
import com.mall.product.dto.SpuQuery;
import com.mall.product.dto.SpuSaveRequest;
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
import com.mall.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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
    /** 下架 */
    public static final int STATUS_OFF_SALE = 0;
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
    private final StockService stockService;

    /**
     * 创建 SPU（含 SKU 与图片），并为每个 SKU 初始化 wms_stock 库存记录。
     * 商品创建后默认下架（status=0），由商家/平台自行上架。
     * shopId 由调用方从 Token 解析，杜绝越权挂到他人店铺。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long shopId, SpuSaveRequest req) {
        if (shopId == null) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        Spu spu = new Spu();
        spu.setName(req.getName());
        spu.setSubtitle(req.getSubtitle());
        spu.setCategoryId(req.getCategoryId());
        spu.setShopId(shopId);
        spu.setBrand(req.getBrand());
        spu.setUnit(req.getUnit());
        spu.setDescription(req.getDescription());
        spu.setMainImage(firstImage(req));
        spu.setStatus(STATUS_OFF_SALE);
        spu.setAuditStatus(AUDIT_PASSED);
        spu.setTotalSales(0);
        spu.setMinPrice(minPrice(req));
        spu.setMaxPrice(maxPrice(req));
        spuMapper.insert(spu);

        saveImages(spu.getId(), req.getImages());

        for (SpuSaveRequest.SkuItem item : req.getSkus()) {
            Sku sku = new Sku();
            sku.setSpuId(spu.getId());
            sku.setSkuCode(item.getSkuCode());
            sku.setName(item.getSpec());
            sku.setPrice(item.getPrice());
            sku.setStock(item.getStock());
            sku.setFrozenStock(0);
            sku.setSales(0);
            sku.setStatus(SKU_ENABLED);
            sku.setSort(item.getSort());
            skuMapper.insert(sku);
            // 同步初始化 DB 库存与 Redis 缓存，保证下单可预占
            stockService.initStock(sku.getId(), spu.getId(), shopId, item.getStock());
        }
        log.info("创建商品: shopId={}, spuId={}, skuCount={}", shopId, spu.getId(), req.getSkus().size());
        return spu.getId();
    }

    /** 商家查看自己店铺的 SPU（含下架商品）。 */
    public PageResult<SpuListItemVO> pageForShop(Long shopId, String keyword, PageQuery pageQuery) {
        Page<Spu> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        var wrapper = Wrappers.<Spu>lambdaQuery()
                .eq(Spu::getShopId, shopId)
                .like(StringUtils.hasText(keyword), Spu::getName, keyword)
                .orderByDesc(Spu::getCreateTime)
                .orderByDesc(Spu::getId);
        return PageResult.of(spuMapper.selectPage(page, wrapper), converter::toListItem);
    }

    private void saveImages(Long spuId, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        for (int i = 0; i < urls.size(); i++) {
            SpuImage img = new SpuImage();
            img.setSpuId(spuId);
            img.setUrl(urls.get(i));
            img.setType(i == 0 ? 1 : 0);
            img.setSort(i);
            spuImageMapper.insert(img);
        }
    }

    private String firstImage(SpuSaveRequest req) {
        if (StringUtils.hasText(req.getMainImage())) {
            return req.getMainImage();
        }
        List<String> images = req.getImages();
        return (images == null || images.isEmpty()) ? null : images.get(0);
    }

    private BigDecimal minPrice(SpuSaveRequest req) {
        return req.getSkus().stream().map(SpuSaveRequest.SkuItem::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(null);
    }

    private BigDecimal maxPrice(SpuSaveRequest req) {
        return req.getSkus().stream().map(SpuSaveRequest.SkuItem::getPrice)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(null);
    }

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
