package com.mall.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "平台创建优惠券请求")
public class CouponSaveRequest {

    @Schema(description = "优惠券名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 128, message = "优惠券名称不能超过128个字符")
    private String name;

    @Schema(description = "类型：1满减 2折扣", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "优惠券类型不能为空")
    private Integer type;

    @Schema(description = "适用店铺ID，为空表示平台通用")
    private Long shopId;

    @Schema(description = "使用门槛金额，默认0")
    @DecimalMin(value = "0.00", message = "门槛金额不能为负数")
    private BigDecimal thresholdAmount;

    @Schema(description = "满减金额（type=1时必填）")
    @DecimalMin(value = "0.00", message = "满减金额不能为负数")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率 0.01~0.99（type=2时必填）")
    @DecimalMin(value = "0.01", message = "折扣率不能小于0.01")
    @DecimalMax(value = "0.99", message = "折扣率不能大于0.99")
    private BigDecimal discountRate;

    @Schema(description = "发行总量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发行总量不能为空")
    @Min(value = 1, message = "发行总量至少为1")
    private Integer totalCount;

    @Schema(description = "每人限领数量，默认1")
    @Min(value = 1, message = "每人限领至少为1")
    private Integer perUserLimit;

    @Schema(description = "生效时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "失效时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "失效时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "使用说明")
    @Size(max = 512, message = "使用说明不能超过512个字符")
    private String description;
}
