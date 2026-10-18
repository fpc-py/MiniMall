package com.mall.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家维护本店信息的请求体。
 */
@Data
@Schema(description = "商家店铺信息修改请求")
public class ShopInfoUpdateRequest {

    @Schema(description = "联系人姓名")
    @Size(max = 64, message = "联系人姓名不能超过64个字符")
    private String contactName;

    @Schema(description = "联系人电话")
    @Size(max = 32, message = "联系人电话不能超过32个字符")
    private String contactPhone;

    @Schema(description = "省")
    @Size(max = 64, message = "省份不能超过64个字符")
    private String province;

    @Schema(description = "市")
    @Size(max = 64, message = "城市不能超过64个字符")
    private String city;

    @Schema(description = "区")
    @Size(max = 64, message = "区县不能超过64个字符")
    private String district;

    @Schema(description = "详细地址")
    @Size(max = 255, message = "详细地址不能超过255个字符")
    private String address;

    @Schema(description = "店铺简介")
    @Size(max = 512, message = "店铺简介不能超过512个字符")
    private String description;
}
