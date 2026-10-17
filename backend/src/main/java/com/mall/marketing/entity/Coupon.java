package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板（pms_coupon）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_coupon")
@Schema(description = "优惠券模板")
public class Coupon extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    /** 1满减 2折扣 */
    @Schema(description = "类型：1满减 2折扣")
    private Integer type;

    @Schema(description = "适用店铺ID，NULL=平台通用")
    private Long shopId;

    @Schema(description = "使用门槛金额")
    private BigDecimal thresholdAmount;

    @Schema(description = "满减金额（type=1）")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率 0.01~0.99（type=2）")
    private BigDecimal discountRate;

    @Schema(description = "发行总量")
    private Integer totalCount;

    @Schema(description = "已领取数量")
    private Integer receivedCount;

    @Schema(description = "每人限领数量")
    private Integer perUserLimit;

    @Schema(description = "生效时间")
    private LocalDateTime startTime;

    @Schema(description = "失效时间")
    private LocalDateTime endTime;

    /** 0停用 1启用 */
    @Schema(description = "状态：0停用 1启用")
    private Integer status;

    @Schema(description = "使用说明")
    private String description;
}
