package com.mall.system.controller;

import com.mall.common.R;
import com.mall.system.dto.SmsLoginRequest;
import com.mall.system.dto.SmsSendRequest;
import com.mall.system.service.AuthService;
import com.mall.system.vo.LoginVO;
import com.mall.system.vo.SmsSendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证 - 短信验证码登录")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "发送登录验证码", description = "同一手机号 60s 内只能发送一次；验证码 60s 过期")
    @PostMapping("/sms/send")
    public R<SmsSendVO> sendSmsCode(@Valid @RequestBody SmsSendRequest request) {
        return R.ok(authService.sendSmsCode(request.getPhone()));
    }

    @Operation(summary = "验证码登录", description = "未注册的手机号自动注册（登录即注册），成功返回 JWT")
    @PostMapping("/sms/login")
    public R<LoginVO> loginBySms(@Valid @RequestBody SmsLoginRequest request) {
        return R.ok(authService.loginBySms(request));
    }
}
