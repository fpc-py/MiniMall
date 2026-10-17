package com.mall.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "商家入驻审核请求")
public class ShopAuditRequest {

    @Schema(description = "入驻申请记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请记录 ID 不能为空")
    private Long applyId;

    @Schema(description = "审核结果：1通过 2拒绝", allowableValues = {"1", "2"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核状态不能为空")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "商家登录账号（审核通过时必填）")
    private String username;

    @Schema(description = "商家初始密码（审核通过时必填，服务端 BCrypt 加密存储）")
    private String password;
}
