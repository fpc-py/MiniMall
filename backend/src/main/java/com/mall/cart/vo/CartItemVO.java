package com.mall.cart.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Redis 中存储的购物车项快照：含商品快照信息，避免每次查购物车都要回查 DB。
 */
@Data
@Schema(description = "购物车项")
public class CartItemVO {

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "SPU ID")
    private Long spuId;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "SKU 名称（规格组合）")
    private String skuName;

    @Schema(description = "SPU 名称")
    private String spuName;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "加购时单价")
    private BigDecimal price;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "是否选中")
    private Boolean selected;

    @Schema(description = "当前可售库存（实时查 DB）")
    private Integer stock;

    @Schema(description = "是否仍有库存")
    private Boolean inStock;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "加购时间")
    private LocalDateTime addTime;
}
