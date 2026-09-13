package com.mall.system.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.R;
import com.mall.common.security.JwtUtils;
import com.mall.common.security.UserType;
import com.mall.system.entity.SysUser;
import com.mall.system.mapper.SysUserMapper;
import com.mall.system.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台端 - 登录认证")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Data
    public static class AdminLoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Operation(summary = "管理后台登录", description = "用户名密码登录，返回JWT token")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, request.getUsername())
                        .eq(SysUser::getStatus, 1));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new MallException(ErrorCode.Auth.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getType() != null && user.getType() > 1) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "无权限登录管理后台");
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), UserType.SYS, user.getShopId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtUtils.getExpiration() / 1000);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName() == null ? user.getUsername() : user.getRealName());
        vo.setUserType("admin");
        return R.ok(vo);
    }
}
