package com.mall.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 购车项实体（cart_item 表）。
 * 运行时以 Redis Hash 为主存储，DB 表作为持久化兜底。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart_item")
public class CartItem extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long memberId;

    private Long spuId;

    private Long skuId;

    private Long shopId;

    private Integer quantity;

    private BigDecimal price;

    /** 1 选中 0 未选 */
    private Integer selected;
}
