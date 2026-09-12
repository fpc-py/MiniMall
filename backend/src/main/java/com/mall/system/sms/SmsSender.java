package com.mall.system.sms;

/**
 * 短信发送通道抽象。接入阿里云/腾讯云短信时新增实现并替换默认 Bean 即可，业务代码无需改动。
 */
public interface SmsSender {

    /**
     * 发送登录验证码。
     *
     * @param phone      手机号
     * @param code       验证码
     * @param ttlSeconds 有效期（秒），用于短信文案提示
     */
    void sendLoginCode(String phone, String code, int ttlSeconds);
}
