package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会员优惠券（pms_coupon_member）：会员领取后的券实例。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_coupon_member")
@Schema(description = "会员优惠券")
public class CouponMember extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "领取记录ID")
    private Long id;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "会员ID")
    private Long memberId;

    /** 0未使用 1已使用 2已过期 */
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
