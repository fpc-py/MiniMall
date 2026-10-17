package com.mall.common.security;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 权限校验切面：按权限码前缀区分平台端与商家端。
 * <p>
 * - {@code admin:*} 仅平台用户（sys 且无 shopId）可访问；
 * - {@code shop:*}  仅商家用户（sys 且带 shopId）可访问；
 * - 其它权限码按系统用户校验（兼容历史接口）。
 * 会员端接口不走本切面（由 {@code @PreAuthorize("hasRole('MEMBER')")} 控制）。
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    private static final String PLATFORM_PREFIX = "admin:";
    private static final String SHOP_PREFIX = "shop:";

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String permission = requirePermission.value();
        LoginUser user = SecurityUtils.currentUser();

        log.info("权限校验：用户 {} 类型 {} 店铺 {} 所需权限 '{}'",
                user.userId(), user.userType(), user.shopId(), permission);

        if (!user.isSys()) {
            log.warn("权限校验失败：用户 {} 非系统用户，无法执行 '{}'", user.userId(), permission);
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }

        // 平台权限：不允许商家越权访问平台功能
        if (permission.startsWith(PLATFORM_PREFIX) && user.shopId() != null) {
            log.warn("权限校验失败：商家用户 {} 越权访问平台权限 '{}'", user.userId(), permission);
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "无权访问平台功能");
        }

        // 商家权限：仅绑定店铺的商家用户可访问
        if (permission.startsWith(SHOP_PREFIX) && user.shopId() == null) {
            log.warn("权限校验失败：平台用户 {} 无店铺，无法执行商家权限 '{}'", user.userId(), permission);
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }

        return joinPoint.proceed();
    }
}