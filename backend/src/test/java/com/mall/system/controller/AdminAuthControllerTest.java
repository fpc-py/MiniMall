package com.mall.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.GlobalExceptionHandler;
import com.mall.common.security.JwtUtils;
import com.mall.common.security.UserType;
import com.mall.system.entity.SysUser;
import com.mall.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 平台/商家后台登录测试：
 * 覆盖「商家账号(type 2/3)可登录后台、userType 映射为 shop」这一修复点。
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminAuthController(sysUserMapper, passwordEncoder, jwtUtils))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SysUser user(int type, Long shopId) {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setUsername("shop");
        u.setPassword("encoded");
        u.setRealName("测试商家");
        u.setStatus(1);
        u.setType(type);
        u.setShopId(shopId);
        return u;
    }

    private String json(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    @Test
    @DisplayName("平台超管(type=0)登录成功：userType=admin")
    void adminLoginSuccess() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(user(0, null));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateToken(anyLong(), anyString(), eq(UserType.SYS), any())).thenReturn("token-x");
        when(jwtUtils.getExpiration()).thenReturn(86400000L);

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("token-x"))
                .andExpect(jsonPath("$.data.userType").value("admin"));
    }

    @Test
    @DisplayName("商家管理员(type=2)登录成功：userType=shop，携带 shopId")
    void merchantLoginSuccess() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(user(2, 1L));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateToken(anyLong(), anyString(), eq(UserType.SYS), eq(1L))).thenReturn("token-shop");
        when(jwtUtils.getExpiration()).thenReturn(86400000L);

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("shop", "shop123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userType").value("shop"));
    }

    @Test
    @DisplayName("密码错误：code=20000")
    void wrongPassword() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(user(0, null));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("admin", "bad")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.Auth.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("非法用户类型(type=9)：code=20001")
    void invalidUserType() throws Exception {
        when(sysUserMapper.selectOne(any())).thenReturn(user(9, null));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ghost", "x")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.Auth.FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("用户名为空：HTTP 400 + code=10000")
    void blankUsername() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()));
    }
}
