package com.mall.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock_lock")
public class StockLock extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long skuId;

    private Long orderId;

    private String orderNo;

    private Integer quantity;

    /** 0 锁定中 1 已出库 2 已释放 3 已取消 */
    private Integer status;

    private LocalDateTime expireTime;
}
