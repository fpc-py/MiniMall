package com.mall.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock")
public class Stock extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long skuId;

    private Long spuId;

    private Long shopId;

    private String warehouseCode;

    private Integer totalStock;

    private Integer availableStock;

    private Integer frozenStock;

    private Integer safetyStock;

    @Version
    private Integer version;
}
