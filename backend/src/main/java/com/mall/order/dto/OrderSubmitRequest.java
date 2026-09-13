package com.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "下单提交请求")
public class OrderSubmitRequest {

    @Schema(description = "收货地址 ID")
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @Schema(description = "直接购买 SKU ID（与 fromCart 二选一）")
    private Long skuId;

    @Schema(description = "直接购买数量")
    private Integer quantity;

    @Schema(description = "是否从购物车结算")
    private Boolean fromCart = true;

    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "确认时的试算令牌（防重复提交）")
    @NotBlank(message = "确认令牌不能为空")
    private String confirmToken;
}
