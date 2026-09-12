package com.mall.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员信息")
public class MemberVO {

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号（已脱敏）", example = "138****8000")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "会员等级")
    private Integer level;

    @Schema(description = "积分")
    private Integer points;
}
