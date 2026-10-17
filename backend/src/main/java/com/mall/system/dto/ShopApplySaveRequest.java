package com.mall.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "商家入驻申请提交请求")
public class ShopApplySaveRequest {

    @Schema(description = "店铺名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 128, message = "店铺名称不能超过128个字符")
    private String shopName;

    @Schema(description = "店铺Logo URL")
    private String shopLogo;

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 64, message = "联系人姓名不能超过64个字符")
    private String contactName;

    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "详细地址")
    @Size(max = 256, message = "详细地址不能超过256个字符")
    private String address;

    @Schema(description = "营业执照图片URL")
    private String businessLicense;

    @Schema(description = "身份证正面URL")
    private String idCardFront;

    @Schema(description = "身份证背面URL")
    private String idCardBack;
}
