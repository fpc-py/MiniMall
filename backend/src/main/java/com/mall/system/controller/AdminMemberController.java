package com.mall.system.controller;

import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.RequirePermission;
import com.mall.system.service.AdminMemberService;
import com.mall.system.vo.AdminMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台端 - 会员管理")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @Operation(summary = "会员列表", description = "平台分页查看会员，keyword 模糊匹配手机号/昵称")
    @GetMapping
    @RequirePermission("admin:member:list")
    public R<PageResult<AdminMemberVO>> page(@Valid PageQuery pageQuery,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(adminMemberService.page(keyword, pageQuery));
    }

    @Operation(summary = "启用/禁用会员", description = "status 1启用 0禁用")
    @PostMapping("/{id}/status")
    @RequirePermission("admin:member:status")
    public R<Void> updateStatus(@PathVariable("id") Long id,
                                @RequestParam Integer status) {
        adminMemberService.updateStatus(id, status);
        return R.ok();
    }
}
