package com.mall.common.security;

/**
 * 登录主体类型：会员端(H5) / 系统端(平台后台、商家后台)。
 */
public enum UserType {

    /** 会员 */
    MEMBER("member"),
    /** 系统用户（平台运营 / 商家） */
    SYS("sys");

    private final String code;

    UserType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** Spring Security 角色名 */
    public String role() {
        return "ROLE_" + name();
    }

    public static UserType of(String code) {
        if (code == null) {
            return null;
        }
        for (UserType type : values()) {
            if (type.code.equalsIgnoreCase(code) || type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
