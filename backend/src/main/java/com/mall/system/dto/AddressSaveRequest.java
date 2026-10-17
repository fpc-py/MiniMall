package com.mall.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "收货地址新增/修改请求")
public class AddressSaveRequest {

    @Schema(description = "收货人姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 64, message = "收货人姓名不能超过64个字符")
    private String receiverName;

    @Schema(description = "收货人手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @Schema(description = "省", example = "广东省", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "省份不能为空")
    private String province;

    @Schema(description = "市", example = "深圳市", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "城市不能为空")
    private String city;

    @Schema(description = "区/县", example = "南山区")
    private String district;

    @Schema(description = "详细地址", example = "科技园路1号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 256, message = "详细地址不能超过256个字符")
    private String detail;

    @Schema(description = "邮编", example = "518000")
    private String zipCode;

    @Schema(description = "地址标签", example = "家")
    private String label;

    @Schema(description = "是否设为默认地址", example = "true")
    private Boolean isDefault;
}
