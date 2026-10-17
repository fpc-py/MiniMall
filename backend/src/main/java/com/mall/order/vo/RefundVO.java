package com.mall.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "退款单视图对象")
public class RefundVO {

    @Schema(description = "退款单ID")
    private Long id;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "补充说明")
    private String description;

    @Schema(description = "凭证图片")
    private List<String> images;

    @Schema(description = "状态：0待审核 1退款中 2已退款 3已拒绝 4已撤销")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "退款到账时间")
    private LocalDateTime refundTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}
