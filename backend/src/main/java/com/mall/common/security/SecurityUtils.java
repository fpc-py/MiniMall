package com.mall.common.security;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 当前登录人读取工具：所有「资源归属校验」都从这里取 userId，禁止信任前端传入的 userId。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<LoginUser> currentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? Optional.of(loginUser) : Optional.empty();
    }

    /** 未登录直接抛 20000 */
    public static LoginUser currentUser() {
        return currentUserOptional()
                .orElseThrow(() -> new MallException(ErrorCode.Auth.UNAUTHORIZED));
    }

    public static Long currentUserId() {
        return currentUser().userId();
    }

    public static boolean isAuthenticated() {
        return currentUserOptional().isPresent();
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }
}
