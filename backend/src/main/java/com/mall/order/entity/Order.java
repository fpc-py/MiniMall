package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order")
public class Order extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long memberId;

    private Long shopId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    /** 1 余额 2 支付宝 3 微信 */
    private Integer payMethod;

    /** 0 未支付 1 已支付 2 已退款 3 部分退款 */
    private Integer payStatus;

    /** 0 待付款 1 待发货 2 已发货 3 已收货 4 已完成 5 已取消 6 售后中 */
    private Integer orderStatus;

    private String deliveryCompany;

    private String deliveryNo;

    private LocalDateTime paymentTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String receiverName;

    private String receiverPhone;

    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverAddress;

    private String memberRemark;

    private String shopRemark;
}
