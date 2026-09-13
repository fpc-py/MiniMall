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
@TableName("wms_outbound_task")
public class OutboundTask extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long shopId;

    private String warehouseCode;

    /** 0 待出库 1 出库中 2 已出库 3 已取消 */
    private Integer status;

    private Integer totalItems;

    private LocalDateTime outboundTime;

    private Long operatorId;

    private String operatorName;
}
