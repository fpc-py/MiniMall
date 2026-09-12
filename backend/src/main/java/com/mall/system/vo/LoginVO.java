package com.mall.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录结果")
public class LoginVO {

    @Schema(description = "JWT，后续请求放到 Authorization: Bearer {token}")
    private String token;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "有效期（秒）", example = "86400")
    private Long expiresIn;

    @Schema(description = "是否本次新注册（登录即注册）")
    private Boolean newMember;

    @Schema(description = "会员信息")
    private MemberVO userInfo;
}
