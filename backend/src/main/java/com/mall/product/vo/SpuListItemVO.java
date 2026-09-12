package com.mall.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "SPU 列表项")
public class SpuListItemVO {

    @Schema(description = "SPU ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题/卖点")
    private String subtitle;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "品牌")
    private String brand;

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
}
