package com.mall.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_shop")
@Schema(description = "店铺")
public class Shop extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "店铺 ID")
    private Long id;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "店铺 Logo")
    private String logo;

    @Schema(description = "店铺横幅")
    private String banner;

    @Schema(description = "店铺简介")
    private String description;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人电话")
    private String contactPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    /** 0待审核 1已上架 2已下架 3审核拒绝 */
    @Schema(description = "店铺状态：0待审核 1已上架 2已下架 3审核拒绝")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;

    /** 0自营 1第三方 */
    @Schema(description = "店铺类型：0自营 1第三方")
    private Integer type;

    @Schema(description = "店铺评分")
    private BigDecimal score;
}