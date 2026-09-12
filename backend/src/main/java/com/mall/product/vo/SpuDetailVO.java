package com.mall.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "SPU 详情")
public class SpuDetailVO {

    @Schema(description = "SPU ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题/卖点")
    private String subtitle;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "商品详情(HTML)")
    private String description;

    @Schema(description = "主图URL")
    private String mainImage;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "最低价")
    private BigDecimal minPrice;

    @Schema(description = "最高价")
    private BigDecimal maxPrice;

    @Schema(description = "总销量")
    private Integer totalSales;

    @Schema(description = "图片列表（0轮播图 1详情图）")
    private List<SpuImageVO> images = new ArrayList<>();

    @Schema(description = "SKU 列表")
    private List<SkuVO> skus = new ArrayList<>();
}
