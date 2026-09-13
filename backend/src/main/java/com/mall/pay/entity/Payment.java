package com.mall.pay.entity;

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
@TableName("pay_payment")
public class Payment extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long memberId;

    private BigDecimal payAmount;

    /** 1 余额 2 支付宝 3 微信 */
    private Integer payMethod;

    /** 第三方交易号（唯一索引，幂等保障） */
    private String transactionNo;

    /** 0 待支付 1 支付成功 2 支付失败 3 已退款 */
    private Integer status;

    private LocalDateTime payTime;

    /** 回调原始内容(JSON) */
    private String callbackContent;

    private BigDecimal refundAmount;

    private LocalDateTime refundTime;
}
