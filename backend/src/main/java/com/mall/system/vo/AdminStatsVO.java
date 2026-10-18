package com.mall.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台仪表盘统计。
 */
@Data
@Schema(description = "平台统计概览")
public class AdminStatsVO {

    @Schema(description = "会员总数")
    private long memberCount;

    @Schema(description = "店铺总数")
    private long shopCount;

    @Schema(description = "订单总数")
    private long orderCount;

    @Schema(description = "今日订单数")
    private long todayOrderCount;

    @Schema(description = "累计销售额（已支付订单金额之和）")
    private BigDecimal totalSalesAmount;

    @Schema(description = "待审核入驻申请数")
    private long pendingShopApplyCount;

    @Schema(description = "待发货订单数")
    private long pendingShipOrderCount;

    @Schema(description = "待处理退款数")
    private long pendingRefundCount;
}
