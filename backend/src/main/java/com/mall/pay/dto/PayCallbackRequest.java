package com.mall.pay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "支付回调请求")
public class PayCallbackRequest {

    @Schema(description = "第三方交易号")
    @NotBlank(message = "交易号不能为空")
    private String transactionNo;

    @Schema(description = "支付状态 1 成功 2 失败")
    @NotNull(message = "支付状态不能为空")
    private Integer status;

    @Schema(description = "回调原始内容(JSON)")
    private String callbackContent;

    @Schema(description = "支付金额（校验用）")
    private java.math.BigDecimal amount;
}
