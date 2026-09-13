package com.mall.pay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "发起支付请求")
public class PayCreateRequest {

    @Schema(description = "订单 ID")
    @NotNull(message = "订单 ID 不能为空")
    private Long orderId;

    @Schema(description = "支付方式 1 余额 2 支付宝 3 微信")
    @NotNull(message = "支付方式不能为空")
    private Integer payMethod;
}
