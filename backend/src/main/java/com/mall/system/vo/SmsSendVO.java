package com.mall.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发送验证码结果")
public class SmsSendVO {

    @Schema(description = "验证码有效期（秒）", example = "60")
    private Integer expireSeconds;

    @Schema(description = "开发联调回显的验证码；接入真实短信通道后为 null", example = "123456")
    private String devCode;

    public static SmsSendVO of(int expireSeconds, String devCode) {
        SmsSendVO vo = new SmsSendVO();
        vo.setExpireSeconds(expireSeconds);
        vo.setDevCode(devCode);
        return vo;
    }
}
