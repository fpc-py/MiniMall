package com.mall.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 对外 SKU：不含成本价，库存只暴露可售数量（stock - frozen_stock）。
 */
@Data
@Schema(description = "SKU 信息")
public class SkuVO {

    @Schema(description = "SKU ID")
    private Long id;

    @Schema(description = "所属 SPU ID")
    private Long spuId;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "SKU 名称（规格组合）")
    private String name;

    @Schema(description = "SKU 图片")
    private String image;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价/划线价")
    private BigDecimal originalPrice;

    @Schema(description = "规格属性", example = "{\"颜色\":\"红色\",\"尺寸\":\"M\"}")
    private Map<String, String> attrs = Collections.emptyMap();

    @Schema(description = "可售库存（已扣除冻结库存）")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;
}
