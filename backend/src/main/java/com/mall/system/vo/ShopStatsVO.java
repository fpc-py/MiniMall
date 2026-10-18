package com.mall.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家端店铺统计概览。
 */
@Data
@Schema(description = "商家统计概览")
public class ShopStatsVO {

    @Schema(description = "本店商品总数")
    private long productCount;

    @Schema(description = "本店订单总数")
    private long orderCount;

    @Schema(description = "待发货订单数")
    private long pendingShipOrderCount;

    @Schema(description = "待处理退款数")
    private long pendingRefundCount;

    @Schema(description = "本店销售额（已支付订单金额之和）")
    private BigDecimal salesAmount;
}
