package com.mall.pay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发起支付结果")
public class PayCreateVO {

    @Schema(description = "支付记录 ID")
    private Long paymentId;

    @Schema(description = "第三方交易号")
    private String transactionNo;

    @Schema(description = "支付页 URL（沙箱模式返回沙箱地址）")
    private String payUrl;

    @Schema(description = "是否创建成功")
    private Boolean success;

    @Schema(description = "提示信息")
    private String message;
}
