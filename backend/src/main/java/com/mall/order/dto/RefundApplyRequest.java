package com.mall.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "买家申请退款请求")
public class RefundApplyRequest {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 256, message = "退款原因不能超过256个字符")
    private String reason;

    @Schema(description = "补充说明")
    @Size(max = 512, message = "补充说明不能超过512个字符")
    private String description;

    @Schema(description = "凭证图片 URL 列表")
    private List<String> images;
}
