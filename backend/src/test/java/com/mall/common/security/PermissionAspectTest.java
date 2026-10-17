package com.mall.common.security;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 权限切面测试：平台(admin:*)与商家(shop:*)权限码前缀区分，杜绝跨端越权。
 */
class PermissionAspectTest {

    private final PermissionAspect aspect = new PermissionAspect();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(UserType type, Long shopId) {
        LoginUser user = new LoginUser(1L, "u1", type, shopId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private RequirePermission perm(String code) {
        RequirePermission rp = mock(RequirePermission.class);
        when(rp.value()).thenReturn(code);
        return rp;
    }

    private ProceedingJoinPoint joinPoint() throws Throwable {
        ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
        when(jp.proceed()).thenReturn("ok");
        return jp;
    }

    @Test
    @DisplayName("会员访问平台权限：抛 20001，不进入业务方法")
    void memberCannotAccessPlatform() throws Throwable {
        login(UserType.MEMBER, null);

        assertThatThrownBy(() -> aspect.checkPermission(joinPoint(), perm("admin:shop:audit")))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("商家越权访问平台权限：抛 20001")
    void shopUserCannotAccessPlatform() throws Throwable {
        login(UserType.SYS, 9L);
        ProceedingJoinPoint jp = joinPoint();

        assertThatThrownBy(() -> aspect.checkPermission(jp, perm("admin:shop:audit")))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());

        verify(jp, never()).proceed();
    }

    @Test
    @DisplayName("平台用户访问平台权限：放行")
    void platformUserCanAccessPlatform() throws Throwable {
        login(UserType.SYS, null);

        Object result = aspect.checkPermission(joinPoint(), perm("admin:shop:audit"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("平台用户访问商家权限：抛 20001")
    void platformUserCannotAccessShop() throws Throwable {
        login(UserType.SYS, null);
        ProceedingJoinPoint jp = joinPoint();

        assertThatThrownBy(() -> aspect.checkPermission(jp, perm("shop:product:create")))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());

        verify(jp, never()).proceed();
    }

    @Test
    @DisplayName("商家访问商家权限：放行")
    void shopUserCanAccessShop() throws Throwable {
        login(UserType.SYS, 9L);

        Object result = aspect.checkPermission(joinPoint(), perm("shop:product:create"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("无前缀历史权限码：系统用户放行")
    void legacyPermissionAllowsSysUser() throws Throwable {
        login(UserType.SYS, 9L);

        Object result = aspect.checkPermission(joinPoint(), perm("legacy:perm"));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("未登录：抛 20000")
    void anonymousRejected() {
        assertThatThrownBy(() -> aspect.checkPermission(mock(ProceedingJoinPoint.class), perm("admin:shop:audit")))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.UNAUTHORIZED.getCode());
    }
}
