package com.mall.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员（user_member）。短信验证码登录即注册，password 为随机不可用占位值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_member")
public class Member extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** BCrypt 密文，任何情况下都不返回给前端 */
    @JsonIgnore
    private String password;

    private String nickname;

    private String phone;

    private String email;

    private String avatar;

    /** 0未知 1男 2女 */
    private Integer gender;

    private LocalDate birthday;

    /** 1启用 0禁用 */
    private Integer status;

    /** 会员等级 0普通 1银 2金 3钻石 */
    private Integer level;

    private Integer points;

    private BigDecimal balance;

    private LocalDateTime lastLoginTime;
}
