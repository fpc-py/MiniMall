package com.mall.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "店铺视图对象")
public class ShopVO {

    @Schema(description = "店铺 ID")
    private Long id;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "店铺 Logo")
    private String logo;

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

    @Schema(description = "店铺状态：0待审核 1已上架 2已下架 3审核拒绝")
    private Integer status;

    @Schema(description = "店铺类型：0自营 1第三方")
    private Integer type;

    @Schema(description = "店铺评分")
    private BigDecimal score;

    @Schema(description = "审核备注")
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}