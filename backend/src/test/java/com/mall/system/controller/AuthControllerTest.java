package com.mall.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.GlobalExceptionHandler;
import com.mall.common.MallException;
import com.mall.system.service.AuthService;
import com.mall.system.vo.LoginVO;
import com.mall.system.vo.MemberVO;
import com.mall.system.vo.SmsSendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口测试：统一 R 返回体 + 参数校验 + 业务错误码。
 * 采用 standalone MockMvc，不启动 Spring 容器（无需 MySQL/Redis）。
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String PHONE = "13800138000";

    @Mock
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @DisplayName("发送验证码成功：code=0，返回有效期与联调验证码")
    void sendSmsCodeSuccess() throws Exception {
        when(authService.sendSmsCode(PHONE)).thenReturn(SmsSendVO.of(60, "123456"));

        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", PHONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.expireSeconds").value(60))
                .andExpect(jsonPath("$.data.devCode").value("123456"));

        verify(authService).sendSmsCode(PHONE);
    }

    @Test
    @DisplayName("手机号格式错误：HTTP 400 + code=10000，不调用业务层")
    void sendSmsCodeInvalidPhone() throws Exception {
        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", "12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()))
                .andExpect(jsonPath("$.msg").value("手机号格式不正确"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("手机号为空：code=10000")
    void sendSmsCodeBlankPhone() throws Exception {
        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("发送过于频繁：业务错误码 20006 以 R 形式返回")
    void sendSmsCodeTooFrequent() throws Exception {
        when(authService.sendSmsCode(PHONE)).thenThrow(new MallException(ErrorCode.Auth.SMS_SEND_TOO_FREQUENT));

        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", PHONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.Auth.SMS_SEND_TOO_FREQUENT.getCode()))
                .andExpect(jsonPath("$.msg").value("验证码发送过于频繁，请稍后再试"));
    }

    @Test
    @DisplayName("验证码登录成功：返回 JWT 与脱敏后的会员信息")
    void loginSuccess() throws Exception {
        MemberVO userInfo = new MemberVO();
        userInfo.setId(9527L);
        userInfo.setUsername("m" + PHONE);
        userInfo.setPhone("138****8000");
        LoginVO loginVO = new LoginVO();
        loginVO.setToken("header.payload.signature");
        loginVO.setTokenType("Bearer");
        loginVO.setExpiresIn(3600L);
        loginVO.setNewMember(true);
        loginVO.setUserInfo(userInfo);
        when(authService.loginBySms(any())).thenReturn(loginVO);

        mockMvc.perform(post("/api/auth/sms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", PHONE, "code", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("header.payload.signature"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.newMember").value(true))
                .andExpect(jsonPath("$.data.userInfo.phone").value("138****8000"))
                .andExpect(jsonPath("$.data.userInfo.password").doesNotExist());

        verify(authService).loginBySms(any());
    }

    @Test
    @DisplayName("验证码错误：code=20004")
    void loginWrongCode() throws Exception {
        when(authService.loginBySms(any())).thenThrow(new MallException(ErrorCode.Auth.SMS_CODE_INVALID));

        mockMvc.perform(post("/api/auth/sms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", PHONE, "code", "654321"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.Auth.SMS_CODE_INVALID.getCode()))
                .andExpect(jsonPath("$.msg").value("验证码错误"));
    }

    @Test
    @DisplayName("验证码格式非法（非6位数字）：HTTP 400 + code=10000")
    void loginInvalidCodeFormat() throws Exception {
        mockMvc.perform(post("/api/auth/sms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", PHONE, "code", "12a456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("请求体缺失：HTTP 400 + code=10000")
    void loginEmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/sms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()));

        verifyNoInteractions(authService);
    }
}
