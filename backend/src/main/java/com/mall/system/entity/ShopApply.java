package com.mall.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商家入驻申请（shop_apply）。
 * 会员自助提交，平台审核；通过后创建 shop_shop 与商家 sys_user 账号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_apply")
@Schema(description = "商家入驻申请")
public class ShopApply extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请人会员ID")
    private Long memberId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺Logo")
    private String shopLogo;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "营业执照图片URL")
    private String businessLicense;

    @Schema(description = "身份证正面")
    private String idCardFront;

    @Schema(description = "身份证背面")
    private String idCardBack;

    /** 0待审核 1通过 2拒绝 */
    @Schema(description = "状态：0待审核 1通过 2拒绝")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核通过后创建的店铺ID")
    private Long shopId;
}
