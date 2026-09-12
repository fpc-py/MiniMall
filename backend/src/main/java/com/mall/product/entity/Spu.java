package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品 SPU（pms_spu）。对外只暴露 status=1 且 audit_status=1 的数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_spu")
public class Spu extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String subtitle;

    private Long categoryId;

    private Long shopId;

    private String brand;

    /** 商品详情(HTML) */
    private String description;

    private String mainImage;

    private String unit;

    /** 0下架 1上架 */
    private Integer status;

    /** 0待审核 1通过 2拒绝 */
    private Integer auditStatus;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer totalSales;
}
