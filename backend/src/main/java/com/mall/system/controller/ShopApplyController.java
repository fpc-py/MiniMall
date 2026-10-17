package com.mall.system.controller;

import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.system.dto.ShopApplySaveRequest;
import com.mall.system.service.ShopApplyService;
import com.mall.system.vo.ShopApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "会员端 - 商家入驻申请")
@RestController
@RequestMapping("/api/shop/apply")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class ShopApplyController {

    private final ShopApplyService shopApplyService;

    @Operation(summary = "提交入驻申请", description = "会员自助提交；同一会员同时只能存在一条待审核/已通过的申请")
    @PostMapping
    public R<ShopApplyVO> submit(@CurrentUser LoginUser user,
                                 @Valid @RequestBody ShopApplySaveRequest req) {
        return R.ok(shopApplyService.submit(user.userId(), req));
    }

    @Operation(summary = "我的入驻申请", description = "memberId 取自 Token")
    @GetMapping("/my")
    public R<List<ShopApplyVO>> my(@CurrentUser LoginUser user) {
        return R.ok(shopApplyService.listByMember(user.userId()));
    }
}
