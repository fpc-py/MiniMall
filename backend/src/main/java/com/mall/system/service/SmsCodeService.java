package com.mall.system.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.sms.SmsProperties;
import com.mall.system.sms.SmsSender;
import com.mall.system.vo.SmsSendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 短信验证码：Redis 存储，默认 60s 过期，同号 60s 内只能发一次。
 *
 * key 约定：
 *   mall:sms:login:code:{phone}  验证码本体，TTL = codeTtlSeconds
 *   mall:sms:login:limit:{phone} 发送频率锁，SETNX + TTL = sendIntervalSeconds
 *   mall:sms:login:fail:{phone}  校验失败计数，超过 maxVerifyFails 作废验证码
 */
@Slf4j
@Service
public class SmsCodeService {

    static final String KEY_CODE = "mall:sms:login:code:";
    static final String KEY_SEND_LIMIT = "mall:sms:login:limit:";
    static final String KEY_FAIL = "mall:sms:login:fail:";

    private final StringRedisTemplate redisTemplate;
    private final SmsSender smsSender;
    private final SmsProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public SmsCodeService(StringRedisTemplate redisTemplate, SmsSender smsSender, SmsProperties properties) {
        this.redisTemplate = redisTemplate;
        this.smsSender = smsSender;
        this.properties = properties;
    }

    /**
     * 发送登录验证码。
     *
     * @return mock 模式下回显验证码，接入真实短信通道后 devCode 为 null
     */
    public SmsSendVO send(String phone) {
        String normalized = normalize(phone);
        // SETNX 原子占位：60s 内重复发送直接被拒，避免短信轰炸
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_SEND_LIMIT + normalized, "1", Duration.ofSeconds(properties.getSendIntervalSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new MallException(ErrorCode.Auth.SMS_SEND_TOO_FREQUENT);
        }
        String code = generateCode();
        redisTemplate.opsForValue().set(KEY_CODE + normalized, code, Duration.ofSeconds(properties.getCodeTtlSeconds()));
        // 重新发码后，旧的失败计数不再有意义
        redisTemplate.delete(KEY_FAIL + normalized);
        smsSender.sendLoginCode(normalized, code, properties.getCodeTtlSeconds());
        return SmsSendVO.of(properties.getCodeTtlSeconds(), properties.isMockEnabled() ? code : null);
    }

    /**
     * 校验验证码：通过后立即删除，保证一码一用（不可重放）。
     */
    public void verify(String phone, String inputCode) {
        String normalized = normalize(phone);
        String codeKey = KEY_CODE + normalized;
        String cached = redisTemplate.opsForValue().get(codeKey);
        if (!StringUtils.hasText(cached)) {
            throw new MallException(ErrorCode.Auth.SMS_CODE_EXPIRED);
        }
        String input = inputCode == null ? "" : inputCode.trim();
        if (!cached.equals(input)) {
            handleWrongCode(normalized, codeKey);
        }
        redisTemplate.delete(codeKey);
        redisTemplate.delete(KEY_FAIL + normalized);
    }

    /** 仅供测试/运维排查使用：读取当前有效验证码 */
    public String peekCode(String phone) {
        return redisTemplate.opsForValue().get(KEY_CODE + normalize(phone));
    }

    private void handleWrongCode(String phone, String codeKey) {
        String failKey = KEY_FAIL + phone;
        Long fails = redisTemplate.opsForValue().increment(failKey);
        if (fails != null && fails == 1L) {
            redisTemplate.expire(failKey, Duration.ofSeconds(properties.getCodeTtlSeconds()));
        }
        if (fails != null && fails >= properties.getMaxVerifyFails()) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(failKey);
            log.warn("验证码错误次数超限，已作废: phone={}", cn.hutool.core.util.DesensitizedUtil.mobilePhone(phone));
            throw new MallException(ErrorCode.Auth.SMS_CODE_FAIL_LIMIT);
        }
        throw new MallException(ErrorCode.Auth.SMS_CODE_INVALID);
    }

    private String generateCode() {
        int length = Math.max(4, Math.min(properties.getCodeLength(), 8));
        int bound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", secureRandom.nextInt(bound));
    }

    private String normalize(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "手机号不能为空");
        }
        String trimmed = phone.trim();
        if (!trimmed.matches("^1[3-9]\\d{9}$")) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "手机号格式不正确");
        }
        return trimmed;
    }
}
