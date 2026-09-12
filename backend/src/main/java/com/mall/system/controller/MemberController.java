package com.mall.system.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.system.service.MemberService;
import com.mall.system.vo.MemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "会员")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "当前登录会员信息", description = "用于验证 JWT 是否生效；userId 取自 Token，不接受前端传参")
    @GetMapping("/me")
    public R<MemberVO> me(@CurrentUser LoginUser loginUser) {
        if (loginUser == null || !loginUser.isMember()) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        return R.ok(memberService.toVO(memberService.getRequiredById(loginUser.userId())));
    }
}
