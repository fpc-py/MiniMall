package com.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "订单发货请求")
public class OrderShipRequest {

    @Schema(description = "订单 ID")
    @NotNull(message = "订单 ID 不能为空")
    private Long orderId;

    @Schema(description = "快递公司")
    @NotBlank(message = "快递公司不能为空")
    private String deliveryCompany;

    @Schema(description = "快递单号")
    @NotBlank(message = "快递单号不能为空")
    private String deliveryNo;
}