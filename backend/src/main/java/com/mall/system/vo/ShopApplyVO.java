package com.mall.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "商家入驻申请视图对象")
public class ShopApplyVO {

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

    @Schema(description = "状态：0待审核 1通过 2拒绝")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核通过后创建的店铺ID")
    private Long shopId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}
