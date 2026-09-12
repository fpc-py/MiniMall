package com.mall.system.sms;

import cn.hutool.core.util.DesensitizedUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认短信通道：只打日志，不真正发短信（v1 未接入短信服务商）。
 * 生产接入真实通道后，用 @Primary 的实现类覆盖本 Bean。
 */
@Slf4j
@Component
public class MockSmsSender implements SmsSender {

    @Override
    public void sendLoginCode(String phone, String code, int ttlSeconds) {
        log.info("【Mock短信】向 {} 发送登录验证码 {}，{} 秒内有效", DesensitizedUtil.mobilePhone(phone), code, ttlSeconds);
    }
}
