package com.mall.cart.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "购物车列表")
public class CartVO {

    @Schema(description = "店铺分组下的购物车项列表")
    private List<ShopGroup> shops;

    @Schema(description = "购物车总件数（所有项的 quantity 之和）")
    private Integer totalQuantity;

    @Schema(description = "选中商品件数")
    private Integer selectedQuantity;

    @Schema(description = "选中商品总金额")
    private BigDecimal selectedAmount;

    @Data
    @Schema(description = "按店铺分组")
    public static class ShopGroup {

        @Schema(description = "店铺 ID")
        private Long shopId;

        @Schema(description = "该店铺下的购物车项")
        private List<CartItemVO> items;
    }
}
