package com.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "商家审核退款请求")
public class RefundAuditRequest {

    @Schema(description = "退款单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退款单ID不能为空")
    private Long refundId;

    @Schema(description = "审核结果：1同意退款 3拒绝", allowableValues = {"1", "3"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;
}
