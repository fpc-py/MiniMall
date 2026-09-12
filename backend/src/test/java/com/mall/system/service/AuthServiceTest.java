package com.mall.system.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.security.JwtUtils;
import com.mall.common.security.LoginUser;
import com.mall.common.security.UserType;
import com.mall.system.dto.SmsLoginRequest;
import com.mall.system.entity.Member;
import com.mall.system.vo.LoginVO;
import com.mall.system.vo.MemberVO;
import com.mall.system.vo.SmsSendVO;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 认证流程测试：验证码校验 -> 登录即注册 -> 签发 JWT。
 * JwtUtils 使用真实实现，确保签发出来的 Token 能被过滤器解析。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";
    private static final String SECRET = "mall-unit-test-secret-key-0123456789abcdef";

    @Mock
    private SmsCodeService smsCodeService;

    @Mock
    private MemberService memberService;

    private final JwtUtils jwtUtils = new JwtUtils(SECRET, 3_600_000L);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(smsCodeService, memberService, jwtUtils);
    }

    private SmsLoginRequest request(String phone, String code) {
        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone(phone);
        request.setCode(code);
        return request;
    }

    private Member member(Long id, int status) {
        Member member = new Member();
        member.setId(id);
        member.setUsername("m" + PHONE);
        member.setPhone(PHONE);
        member.setNickname("用户8000");
        member.setStatus(status);
        return member;
    }

    private MemberVO memberVO(Long id) {
        MemberVO vo = new MemberVO();
        vo.setId(id);
        vo.setUsername("m" + PHONE);
        vo.setPhone("138****8000");
        return vo;
    }

    @Test
    @DisplayName("新用户验证码登录：先校验验证码再落库，返回可解析的会员 JWT")
    void loginBySmsRegistersAndIssuesToken() {
        Member member = member(9527L, 1);
        MemberVO vo = memberVO(9527L);
        when(memberService.getOrCreateByPhone(PHONE)).thenReturn(new MemberService.MemberResult(member, true));
        when(memberService.isEnabled(member)).thenReturn(true);
        when(memberService.toVO(member)).thenReturn(vo);

        LoginVO login = authService.loginBySms(request(PHONE, CODE));

        InOrder inOrder = inOrder(smsCodeService, memberService);
        inOrder.verify(smsCodeService).verify(PHONE, CODE);
        inOrder.verify(memberService).getOrCreateByPhone(PHONE);
        inOrder.verify(memberService).touchLastLogin(9527L);

        assertThat(login.getToken()).isNotBlank();
        assertThat(login.getTokenType()).isEqualTo("Bearer");
        assertThat(login.getExpiresIn()).isEqualTo(3600L);
        assertThat(login.getNewMember()).isTrue();
        assertThat(login.getUserInfo()).isSameAs(vo);

        // Token 可被 JWT 过滤器解析，身份为会员
        assertThat(jwtUtils.validateToken(login.getToken())).isTrue();
        Claims claims = jwtUtils.parseToken(login.getToken());
        LoginUser loginUser = jwtUtils.toLoginUser(claims);
        assertThat(loginUser.userId()).isEqualTo(9527L);
        assertThat(loginUser.username()).isEqualTo("m" + PHONE);
        assertThat(loginUser.userType()).isEqualTo(UserType.MEMBER);
        // Token 中不含手机号等敏感明文
        assertThat(claims.get("phone")).isNull();
    }

    @Test
    @DisplayName("老用户登录：newMember=false，不重复注册")
    void loginBySmsExistingMember() {
        Member member = member(1001L, 1);
        when(memberService.getOrCreateByPhone(PHONE)).thenReturn(new MemberService.MemberResult(member, false));
        when(memberService.isEnabled(member)).thenReturn(true);
        when(memberService.toVO(member)).thenReturn(memberVO(1001L));

        LoginVO login = authService.loginBySms(request(PHONE, CODE));

        assertThat(login.getNewMember()).isFalse();
        assertThat(jwtUtils.getUserId(login.getToken())).isEqualTo(1001L);
        verify(memberService).touchLastLogin(1001L);
    }

    @Test
    @DisplayName("验证码错误：不查库、不建号、不发 Token")
    void loginBySmsWrongCodeStopsBeforeDatabase() {
        doThrow(new MallException(ErrorCode.Auth.SMS_CODE_INVALID))
                .when(smsCodeService).verify(PHONE, "000000");

        assertThatThrownBy(() -> authService.loginBySms(request(PHONE, "000000")))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.SMS_CODE_INVALID.getCode());

        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("验证码过期：抛 20005，不签发 Token")
    void loginBySmsExpiredCode() {
        doThrow(new MallException(ErrorCode.Auth.SMS_CODE_EXPIRED))
                .when(smsCodeService).verify(PHONE, CODE);

        assertThatThrownBy(() -> authService.loginBySms(request(PHONE, CODE)))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.SMS_CODE_EXPIRED.getCode());

        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("被禁用账号：抛 20003，且不更新登录时间")
    void loginBySmsDisabledMember() {
        Member disabled = member(2002L, 0);
        when(memberService.getOrCreateByPhone(PHONE)).thenReturn(new MemberService.MemberResult(disabled, false));
        when(memberService.isEnabled(disabled)).thenReturn(false);

        assertThatThrownBy(() -> authService.loginBySms(request(PHONE, CODE)))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.ACCOUNT_DISABLED.getCode());

        verify(memberService, never()).touchLastLogin(anyLong());
        verify(memberService, never()).toVO(any());
    }

    @Test
    @DisplayName("手机号格式非法：抛 10000，不触发验证码校验")
    void loginBySmsInvalidPhone() {
        assertThatThrownBy(() -> authService.loginBySms(request("12345", CODE)))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());

        verifyNoInteractions(smsCodeService, memberService);
    }

    @Test
    @DisplayName("手机号前后空格自动忽略")
    void loginBySmsTrimsPhone() {
        Member member = member(9527L, 1);
        when(memberService.getOrCreateByPhone(PHONE)).thenReturn(new MemberService.MemberResult(member, false));
        when(memberService.isEnabled(member)).thenReturn(true);
        when(memberService.toVO(member)).thenReturn(memberVO(9527L));

        LoginVO login = authService.loginBySms(request("  " + PHONE + " ", CODE));

        verify(smsCodeService).verify(PHONE, CODE);
        assertThat(login.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("空请求体：抛 10001")
    void loginBySmsNullRequest() {
        assertThatThrownBy(() -> authService.loginBySms(null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.MISSING_PARAM.getCode());
    }

    @Test
    @DisplayName("发送验证码委托给 SmsCodeService")
    void sendSmsCodeDelegates() {
        SmsSendVO sendVO = SmsSendVO.of(60, "123456");
        when(smsCodeService.send(PHONE)).thenReturn(sendVO);

        assertThat(authService.sendSmsCode(" " + PHONE + " ")).isSameAs(sendVO);
        verify(smsCodeService).send(PHONE);
    }
}
