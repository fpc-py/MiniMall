package com.mall.common.security;

import java.io.Serializable;

/**
 * 认证主体：只放非敏感标识，不含手机号/密码等敏感明文。
 *
 * @param userId   会员ID 或 系统用户ID
 * @param username 用户名
 * @param userType 主体类型
 */
public record LoginUser(Long userId, String username, UserType userType) implements Serializable {

    public boolean isMember() {
        return userType == UserType.MEMBER;
    }

    public boolean isSys() {
        return userType == UserType.SYS;
    }

    public String role() {
        return userType == null ? null : userType.role();
    }
}
