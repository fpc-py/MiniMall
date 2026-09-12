package com.mall.common.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 过滤器测试：验证登录态是否正确写入 SecurityContext，以及非法 Token 不会污染上下文。
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "mall-unit-test-secret-key-0123456789abcdef";

    private final JwtUtils jwtUtils = new JwtUtils(SECRET, 3_600_000L);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/member/me");
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        return request;
    }

    private MockFilterChain doFilter(MockHttpServletRequest request) throws ServletException, IOException {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    @DisplayName("合法会员 Token：写入 LoginUser 主体与 ROLE_MEMBER 权限")
    void validMemberTokenAuthenticates() throws Exception {
        String token = jwtUtils.generateToken(9527L, "m13800138000", UserType.MEMBER);

        MockFilterChain chain = doFilter(request("Bearer " + token));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(LoginUser.class);

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        assertThat(loginUser.userId()).isEqualTo(9527L);
        assertThat(loginUser.username()).isEqualTo("m13800138000");
        assertThat(loginUser.userType()).isEqualTo(UserType.MEMBER);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MEMBER");
        // 认证成功也要继续放行到后续过滤器
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("合法系统用户 Token：授予 ROLE_SYS")
    void validSysTokenAuthenticates() throws Exception {
        String token = jwtUtils.generateToken(1L, "admin", UserType.SYS);

        doFilter(request("Bearer " + token));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(((LoginUser) authentication.getPrincipal()).isSys()).isTrue();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SYS");
    }

    @Test
    @DisplayName("没有 Authorization 头：保持匿名，请求继续")
    void missingHeaderKeepsAnonymous() throws Exception {
        MockFilterChain chain = doFilter(request(null));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("缺少 Bearer 前缀：不认证")
    void headerWithoutBearerPrefixIsIgnored() throws Exception {
        String token = jwtUtils.generateToken(9527L, "m13800138000", UserType.MEMBER);

        doFilter(request(token));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("伪造/非法 Token：不认证且不抛异常（由 EntryPoint 返回 20000）")
    void invalidTokenKeepsAnonymous() throws Exception {
        MockFilterChain chain = doFilter(request("Bearer abc.def.ghi"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("过期 Token：不认证")
    void expiredTokenKeepsAnonymous() throws Exception {
        String token = new JwtUtils(SECRET, -1_000L).generateToken(9527L, "m13800138000", UserType.MEMBER);

        doFilter(request("Bearer " + token));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("已存在登录态时，非法 Token 会清空上下文（防止串号）")
    void invalidTokenClearsExistingContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser(1L, "stale", UserType.MEMBER), null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))));

        doFilter(request("Bearer invalid-token"));

        assertThat(SecurityUtils.currentUserOptional()).isEmpty();
    }
}
