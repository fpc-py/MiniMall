package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品 SKU（pms_sku）。costPrice 为内部成本价，禁止出现在任何对外 VO 中。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_sku")
public class Sku extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long spuId;

    private String skuCode;

    private String name;

    private String image;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 成本价：仅内部使用 */
    private BigDecimal costPrice;

    private BigDecimal weight;

    private BigDecimal volume;

    /** 规格属性 JSON，如 {"颜色":"红色","尺寸":"M"} */
    private String attrs;

    private Integer stock;

    /** 冻结库存（已下单未支付） */
    private Integer frozenStock;

    private Integer sales;

    /** 1启用 0禁用 */
    private Integer status;

    private Integer sort;
}
