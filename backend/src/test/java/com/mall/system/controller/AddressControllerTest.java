package com.mall.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.GlobalExceptionHandler;
import com.mall.common.MallException;
import com.mall.common.security.CurrentUserArgumentResolver;
import com.mall.common.security.LoginUser;
import com.mall.common.security.UserType;
import com.mall.system.service.AddressService;
import com.mall.system.vo.AddressVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 收货地址接口测试：统一 R 返回体、参数校验、错误码、userId 取自 Token。
 */
@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    private static final long MEMBER_ID = 1L;

    @Mock
    private AddressService addressService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AddressController(addressService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
        // 模拟已登录会员（userId=1）
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(MEMBER_ID, "m138", UserType.MEMBER), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AddressVO sampleVO() {
        AddressVO vo = new AddressVO();
        vo.setId(10L);
        vo.setReceiverName("张三");
        vo.setReceiverPhone("13800138000");
        vo.setProvince("广东省");
        vo.setCity("深圳市");
        vo.setDistrict("南山区");
        vo.setDetail("科技园路1号");
        vo.setIsDefault(1);
        vo.setFullAddress("广东省深圳市南山区科技园路1号");
        return vo;
    }

    private String validBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "receiverName", "张三",
                "receiverPhone", "13800138000",
                "province", "广东省",
                "city", "深圳市",
                "district", "南山区",
                "detail", "科技园路1号",
                "isDefault", true));
    }

    @Test
    @DisplayName("列表：userId 取自 Token，返回 R.ok")
    void list() throws Exception {
        when(addressService.list(MEMBER_ID)).thenReturn(List.of(sampleVO()));

        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].fullAddress").value("广东省深圳市南山区科技园路1号"));

        verify(addressService).list(MEMBER_ID);
    }

    @Test
    @DisplayName("详情：地址不存在返回 30018")
    void detailNotFound() throws Exception {
        when(addressService.get(MEMBER_ID, 99L)).thenThrow(new MallException(ErrorCode.Biz.ADDRESS_NOT_FOUND));

        mockMvc.perform(get("/api/addresses/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.Biz.ADDRESS_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.msg").value("收货地址不存在"));
    }

    @Test
    @DisplayName("详情：越权访问他人地址返回 20001")
    void detailForbidden() throws Exception {
        when(addressService.get(MEMBER_ID, 5L)).thenThrow(new MallException(ErrorCode.Auth.FORBIDDEN, "无权操作该收货地址"));

        mockMvc.perform(get("/api/addresses/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.Auth.FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("新增：成功返回 R.ok，memberId 由 Token 注入")
    void create() throws Exception {
        when(addressService.create(eq(MEMBER_ID), any())).thenReturn(sampleVO());

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(addressService).create(eq(MEMBER_ID), any());
    }

    @Test
    @DisplayName("新增：缺少收货人姓名返回 HTTP 400 + 10000，不调用业务层")
    void createMissingReceiverName() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "receiverPhone", "13800138000",
                "province", "广东省",
                "city", "深圳市",
                "detail", "科技园路1号"));

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()));

        verifyNoInteractions(addressService);
    }

    @Test
    @DisplayName("新增：手机号格式错误返回 HTTP 400 + 10000")
    void createInvalidPhone() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "receiverName", "张三",
                "receiverPhone", "12345",
                "province", "广东省",
                "city", "深圳市",
                "detail", "科技园路1号"));

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()));

        verifyNoInteractions(addressService);
    }

    @Test
    @DisplayName("修改：成功返回 R.ok")
    void update() throws Exception {
        when(addressService.update(eq(MEMBER_ID), eq(10L), any())).thenReturn(sampleVO());

        mockMvc.perform(put("/api/addresses/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(addressService).update(eq(MEMBER_ID), eq(10L), any());
    }

    @Test
    @DisplayName("删除：成功返回 R.ok")
    void deleteAddress() throws Exception {
        mockMvc.perform(delete("/api/addresses/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(addressService).delete(MEMBER_ID, 10L);
    }

    @Test
    @DisplayName("设为默认：成功返回 R.ok")
    void setDefault() throws Exception {
        mockMvc.perform(put("/api/addresses/10/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(addressService).setDefault(MEMBER_ID, 10L);
    }
}
