package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_item")
public class OrderItem extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long spuId;

    private Long skuId;

    private String skuCode;

    private String productName;

    private String productImage;

    /** 规格属性快照 JSON */
    private String attrs;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal subtotal;
}
