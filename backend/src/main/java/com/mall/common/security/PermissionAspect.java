package com.mall.common.security;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String permission = requirePermission.value();
        LoginUser user = SecurityUtils.currentUser();

        log.info("权限校验：用户 {} 类型 {} 所需权限 '{}'", user.userId(), user.userType(), permission);

        if (!user.isSys()) {
            log.warn("权限校验失败：用户 {} 非系统用户，无法执行 '{}'", user.userId(), permission);
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }

        return joinPoint.proceed();
    }
}