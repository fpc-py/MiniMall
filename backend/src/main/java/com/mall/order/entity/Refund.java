package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款/售后单（oms_refund）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_refund")
@Schema(description = "退款单")
public class Refund extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "退款单ID")
    private Long id;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "申请会员ID")
    private Long memberId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "补充说明")
    private String description;

    @Schema(description = "凭证图片URL，逗号分隔")
    private String images;

    /** 0待审核 1退款中 2已退款 3已拒绝 4已撤销 */
    @Schema(description = "状态：0待审核 1退款中 2已退款 3已拒绝 4已撤销")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "退款到账时间")
    private LocalDateTime refundTime;

    @Schema(description = "退款渠道流水号")
    private String transactionNo;
}
