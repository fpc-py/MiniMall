package com.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "优惠券信息")
public class CouponVO {

    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型：1满减 2折扣")
    private Integer type;

    @Schema(description = "适用店铺ID，NULL=平台通用")
    private Long shopId;

    @Schema(description = "使用门槛金额")
    private BigDecimal thresholdAmount;

    @Schema(description = "满减金额")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "发行总量")
    private Integer totalCount;

    @Schema(description = "已领取数量")
    private Integer receivedCount;

    @Schema(description = "剩余可领数量")
    private Integer remainCount;

    @Schema(description = "每人限领数量")
    private Integer perUserLimit;

    @Schema(description = "生效时间")
    private LocalDateTime startTime;

    @Schema(description = "失效时间")
    private LocalDateTime endTime;

    @Schema(description = "状态：0停用 1启用")
    private Integer status;

    @Schema(description = "使用说明")
    private String description;

    @Schema(description = "当前会员是否已领取")
    private Boolean received;
}
