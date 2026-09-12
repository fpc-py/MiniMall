package com.mall.system.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.sms.SmsProperties;
import com.mall.system.sms.SmsSender;
import com.mall.system.vo.SmsSendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 短信验证码服务测试：Redis 存 code、60s 过期、发送限流、一码一用、防暴力猜码。
 * 使用 Mockito 模拟 Redis，不依赖真实中间件。
 */
@ExtendWith(MockitoExtension.class)
class SmsCodeServiceTest {

    private static final String PHONE = "13800138000";
    private static final String CODE_KEY = "mall:sms:login:code:" + PHONE;
    private static final String LIMIT_KEY = "mall:sms:login:limit:" + PHONE;
    private static final String FAIL_KEY = "mall:sms:login:fail:" + PHONE;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SmsSender smsSender;

    private SmsProperties properties;
    private SmsCodeService smsCodeService;

    @BeforeEach
    void setUp() {
        properties = new SmsProperties();
        properties.setCodeTtlSeconds(60);
        properties.setSendIntervalSeconds(60);
        properties.setCodeLength(6);
        properties.setMaxVerifyFails(5);
        properties.setMockEnabled(true);
        smsCodeService = new SmsCodeService(redisTemplate, smsSender, properties);
    }

    @Test
    @DisplayName("发送验证码：写入 Redis 且 TTL 为 60 秒，码为 6 位数字")
    void sendStoresCodeInRedisWith60sTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(LIMIT_KEY, "1", Duration.ofSeconds(60))).thenReturn(true);

        SmsSendVO result = smsCodeService.send(PHONE);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(CODE_KEY), codeCaptor.capture(), eq(Duration.ofSeconds(60)));
        String code = codeCaptor.getValue();

        assertThat(code).matches("^\\d{6}$");
        assertThat(result.getExpireSeconds()).isEqualTo(60);
        assertThat(result.getDevCode()).isEqualTo(code);
        verify(smsSender).sendLoginCode(PHONE, code, 60);
        // 重新发码要清掉历史失败计数
        verify(redisTemplate).delete(FAIL_KEY);
    }

    @Test
    @DisplayName("发送限流：60 秒内重复发送被拒，不产生新验证码")
    void sendIsRateLimitedWithin60Seconds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(LIMIT_KEY, "1", Duration.ofSeconds(60))).thenReturn(false);

        assertThatThrownBy(() -> smsCodeService.send(PHONE))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.SMS_SEND_TOO_FREQUENT.getCode());

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        verifyNoInteractions(smsSender);
    }

    @Test
    @DisplayName("手机号非法：直接参数错误，不触碰 Redis")
    void sendRejectsInvalidPhone() {
        assertThatThrownBy(() -> smsCodeService.send("12345"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());

        verifyNoInteractions(redisTemplate, smsSender);
    }

    @Test
    @DisplayName("关闭 mock 后接口不回显验证码，但短信通道仍收到真实验证码")
    void devCodeIsHiddenWhenMockDisabled() {
        properties.setMockEnabled(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(LIMIT_KEY, "1", Duration.ofSeconds(60))).thenReturn(true);

        SmsSendVO result = smsCodeService.send(PHONE);

        assertThat(result.getDevCode()).isNull();
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsSender).sendLoginCode(eq(PHONE), codeCaptor.capture(), anyInt());
        assertThat(codeCaptor.getValue()).matches("^\\d{6}$");
    }

    @Test
    @DisplayName("校验通过：验证码立即删除，保证一码一用")
    void verifySuccessConsumesCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn("123456");

        smsCodeService.verify(PHONE, "123456");

        verify(redisTemplate).delete(CODE_KEY);
        verify(redisTemplate).delete(FAIL_KEY);
    }

    @Test
    @DisplayName("验证码错误：抛 20004，且不删除验证码（有效期内允许重试）")
    void verifyWrongCodeKeepsCodeForRetry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn("123456");
        when(valueOperations.increment(FAIL_KEY)).thenReturn(1L);

        assertThatThrownBy(() -> smsCodeService.verify(PHONE, "654321"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.SMS_CODE_INVALID.getCode());

        verify(redisTemplate, never()).delete(CODE_KEY);
        verify(redisTemplate).expire(FAIL_KEY, Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("验证码过期/未发送：抛 20005")
    void verifyExpiredCodeFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn(null);

        assertThatThrownBy(() -> smsCodeService.verify(PHONE, "123456"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.SMS_CODE_EXPIRED.getCode());

        verify(redisTemplate, never()).delete(CODE_KEY);
    }

    @Test
    @DisplayName("错误次数超限：作废验证码并抛 20007，防止暴力猜码")
    void verifyTooManyFailsInvalidatesCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn("123456");
        when(valueOperations.increment(FAIL_KEY)).thenReturn(5L);

        assertThatThrownBy(() -> smsCodeService.verify(PHONE, "000000"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.SMS_CODE_FAIL_LIMIT.getCode());

        verify(redisTemplate).delete(CODE_KEY);
        verify(redisTemplate).delete(FAIL_KEY);
    }

    @Test
    @DisplayName("验证码前后空格自动忽略")
    void verifyTrimsInputCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CODE_KEY)).thenReturn("123456");

        smsCodeService.verify(PHONE, " 123456 ");

        verify(redisTemplate).delete(CODE_KEY);
    }

    @Test
    @DisplayName("连续两次发送产生不同验证码（随机性）")
    void generatedCodesAreRandom() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        smsCodeService.send(PHONE);
        smsCodeService.send(PHONE);

        verify(valueOperations, times(2)).set(eq(CODE_KEY), codeCaptor.capture(), any(Duration.class));
        assertThat(codeCaptor.getAllValues()).hasSize(2);
        assertThat(codeCaptor.getAllValues().get(0)).isNotEqualTo(codeCaptor.getAllValues().get(1));
    }
}
