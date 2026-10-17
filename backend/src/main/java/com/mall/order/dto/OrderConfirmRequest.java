package com.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "下单确认请求（试算价格）")
public class OrderConfirmRequest {

    @Schema(description = "收货地址 ID")
    private Long addressId;

    @Schema(description = "直接购买 SKU ID（与 fromCart 二选一）")
    private Long skuId;

    @Schema(description = "直接购买数量（配合 skuId 使用）")
    private Integer quantity;

    @Schema(description = "是否从购物车结算")
    private Boolean fromCart = true;

    @Schema(description = "使用的会员优惠券记录ID，可为空")
    private Long couponMemberId;

    @Schema(description = "买家备注")
    private String remark;
}
