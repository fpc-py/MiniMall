package com.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "会员优惠券（我的券）")
public class CouponMemberVO {

    @Schema(description = "领取记录ID")
    private Long id;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "优惠券名称")
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

    @Schema(description = "状态：0未使用 1已使用 2已过期")
    private Integer status;

    @Schema(description = "使用的订单ID")
    private Long orderId;

    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
