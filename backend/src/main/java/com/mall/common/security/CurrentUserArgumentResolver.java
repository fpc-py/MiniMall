package com.mall.common.security;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 支持在 Controller 方法上直接声明 LoginUser 参数（可配合 @CurrentUser 控制是否必须登录）。
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return LoginUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation == null || annotation.required();
        LoginUser loginUser = SecurityUtils.currentUserOptional().orElse(null);
        if (loginUser == null && required) {
            throw new MallException(ErrorCode.Auth.UNAUTHORIZED);
        }
        return loginUser;
    }
}
