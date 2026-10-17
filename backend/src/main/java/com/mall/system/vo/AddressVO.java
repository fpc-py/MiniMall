package com.mall.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "收货地址")
public class AddressVO {

    @Schema(description = "地址ID")
    private Long id;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人手机号")
    private String receiverPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "详细地址")
    private String detail;

    @Schema(description = "邮编")
    private String zipCode;

    @Schema(description = "地址标签")
    private String label;

    @Schema(description = "是否默认 1是 0否")
    private Integer isDefault;

    @Schema(description = "完整地址（省市区+详细）")
    private String fullAddress;
}
