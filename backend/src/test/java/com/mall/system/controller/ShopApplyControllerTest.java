package com.mall.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.GlobalExceptionHandler;
import com.mall.common.MallException;
import com.mall.common.security.CurrentUserArgumentResolver;
import com.mall.common.security.LoginUser;
import com.mall.common.security.UserType;
import com.mall.system.service.ShopApplyService;
import com.mall.system.vo.ShopApplyVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商家入驻申请接口测试：统一 R 返回体、参数校验、错误码、memberId 取自 Token。
 */
@ExtendWith(MockitoExtension.class)
class ShopApplyControllerTest {

    private static final long MEMBER_ID = 1L;

    @Mock
    private ShopApplyService shopApplyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ShopApplyController(shopApplyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(MEMBER_ID, "m138", UserType.MEMBER), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ShopApplyVO sampleVO() {
        ShopApplyVO vo = new ShopApplyVO();
        vo.setId(100L);
        vo.setMemberId(MEMBER_ID);
        vo.setShopName("小米旗舰店");
        vo.setStatus(ShopApplyService.APPLY_PENDING);
        return vo;
    }

    private String validBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "shopName", "小米旗舰店",
                "contactName", "王经理",
                "contactPhone", "13800138000",
                "province", "广东省",
                "city", "深圳市",
                "address", "科技园路1号"));
    }

    @Test
    @DisplayName("提交：成功返回 R.ok，memberId 由 Token 注入")
    void submit() throws Exception {
        when(shopApplyService.submit(eq(MEMBER_ID), any())).thenReturn(sampleVO());

        mockMvc.perform(post("/api/shop/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.status").value(0));

        verify(shopApplyService).submit(eq(MEMBER_ID), any());
    }

    @Test
    @DisplayName("提交：缺少店铺名称返回 HTTP 400 + 10000，不调用业务层")
    void submitMissingShopName() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "contactName", "王经理",
                "contactPhone", "13800138000"));

        mockMvc.perform(post("/api/shop/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.Param.INVALID_PARAM.getCode()));

        verifyNoInteractions(shopApplyService);
    }

    @Test
    @DisplayName("提交：重复申请返回 30019")
    void submitDuplicate() throws Exception {
        when(shopApplyService.submit(eq(MEMBER_ID), any()))
                .thenThrow(new MallException(ErrorCode.Biz.SHOP_APPLY_EXISTS));

        mockMvc.perform(post("/api/shop/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.Biz.SHOP_APPLY_EXISTS.getCode()));
    }

    @Test
    @DisplayName("我的申请：userId 取自 Token，返回列表")
    void myApplies() throws Exception {
        when(shopApplyService.listByMember(MEMBER_ID)).thenReturn(List.of(sampleVO()));

        mockMvc.perform(get("/api/shop/apply/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].shopName").value("小米旗舰店"));

        verify(shopApplyService).listByMember(MEMBER_ID);
    }
}
