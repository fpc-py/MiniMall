package com.mall.system.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String username;
    private String realName;
    private String userType;
    private Boolean newMember;
    private MemberVO userInfo;
}
