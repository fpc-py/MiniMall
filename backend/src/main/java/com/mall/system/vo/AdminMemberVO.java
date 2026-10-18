package com.mall.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台端会员视图对象：运营需要看到完整手机号与账户状态，因此不做脱敏。
 */
@Data
@Schema(description = "平台端会员信息")
public class AdminMemberVO {

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "会员等级")
    private Integer level;

    @Schema(description = "积分")
    private Integer points;

    @Schema(description = "余额")
    private BigDecimal balance;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
