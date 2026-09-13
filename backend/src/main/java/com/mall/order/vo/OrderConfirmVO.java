package com.mall.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "下单确认（试算）结果")
public class OrderConfirmVO {

    @Schema(description = "确认令牌（提交时携带，防重复提交）")
    private String confirmToken;

    @Schema(description = "商品项列表")
    private List<OrderItemVO> items;

    @Schema(description = "收货信息")
    private ReceiverVO receiver;

    @Schema(description = "商品总金额")
    private BigDecimal totalAmount;

    @Schema(description = "运费")
    private BigDecimal freightAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "总件数")
    private Integer totalQuantity;

    @Data
    @Schema(description = "下单商品项")
    public static class OrderItemVO {

        @Schema(description = "SKU ID")
        private Long skuId;

        @Schema(description = "SPU ID")
        private Long spuId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String productImage;

        @Schema(description = "规格属性")
        private String attrs;

        @Schema(description = "单价")
        private BigDecimal price;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "小计")
        private BigDecimal subtotal;

        @Schema(description = "当前可售库存")
        private Integer stock;
    }

    @Data
    @Schema(description = "收货信息")
    public static class ReceiverVO {

        @Schema(description = "收货人")
        private String name;

        @Schema(description = "联系电话")
        private String phone;

        @Schema(description = "省")
        private String province;

        @Schema(description = "市")
        private String city;

        @Schema(description = "区")
        private String district;

        @Schema(description = "详细地址")
        private String address;
    }
}
