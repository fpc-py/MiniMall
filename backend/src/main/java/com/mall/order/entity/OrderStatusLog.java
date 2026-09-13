package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_status_log")
public class OrderStatusLog extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Integer fromStatus;

    private Integer toStatus;

    /** 0 系统 1 用户 2 商家 3 管理员 */
    private Integer operatorType;

    private Long operatorId;

    private String operatorName;

    private String content;
}
