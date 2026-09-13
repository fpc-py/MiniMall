package com.mall.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "店铺审核请求")
public class ShopAuditRequest {

    @Schema(description = "申请记录 ID")
    @NotNull(message = "申请记录 ID 不能为空")
    private Long applyId;

    @Schema(description = "审核结果：1通过 2拒绝", allowableValues = {"1", "2"})
    @NotNull(message = "审核状态不能为空")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;
}