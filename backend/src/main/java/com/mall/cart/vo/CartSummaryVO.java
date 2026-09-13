package com.mall.cart.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "购物车合计")
public class CartSummaryVO {

    @Schema(description = "购物车总件数（所有项的 quantity 之和）")
    private Integer totalQuantity;

    @Schema(description = "选中商品件数")
    private Integer selectedQuantity;

    @Schema(description = "选中商品总金额")
    private BigDecimal selectedAmount;

    @Schema(description = "选中商品种类数")
    private Integer selectedCount;
}
