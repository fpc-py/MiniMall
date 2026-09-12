package com.mall.system.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信验证码配置（mall.sms.*）。
 * 需求约定：验证码 60s 过期、同号 60s 内只能发送一次。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.sms")
public class SmsProperties {

    /** 未接入真实短信通道时为 true：接口回显验证码，仅用于本地联调，生产必须置为 false */
    private boolean mockEnabled = true;

    /** 验证码位数 */
    private int codeLength = 6;

    /** 验证码有效期（秒） */
    private int codeTtlSeconds = 60;

    /** 同一手机号两次发送的最小间隔（秒） */
    private int sendIntervalSeconds = 60;

    /** 同一验证码允许的最大校验失败次数，超过即作废，防止暴力猜码 */
    private int maxVerifyFails = 5;
}
