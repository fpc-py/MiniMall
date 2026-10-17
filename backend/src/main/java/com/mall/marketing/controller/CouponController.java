package com.mall.marketing.controller;

import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.marketing.service.CouponService;
import com.mall.marketing.vo.CouponMemberVO;
import com.mall.marketing.vo.CouponVO;
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

import java.util.List;

@Tag(name = "买家端 - 优惠券")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "领券中心", description = "当前会员可领取的优惠券列表")
    @GetMapping("/available")
    public R<List<CouponVO>> available(@CurrentUser LoginUser user) {
        return R.ok(couponService.listAvailable(user.userId()));
    }

    @Operation(summary = "领取优惠券", description = "会员领取一张优惠券，受总量与个人限领约束")
    @PostMapping("/{couponId}/receive")
    public R<CouponMemberVO> receive(@CurrentUser LoginUser user,
                                     @PathVariable("couponId") Long couponId) {
        return R.ok(couponService.receive(user.userId(), couponId));
    }

    @Operation(summary = "我的优惠券", description = "memberId 取自 Token，可按状态筛选")
    @GetMapping("/my")
    public R<PageResult<CouponMemberVO>> myCoupons(@CurrentUser LoginUser user,
                                                   @Valid PageQuery pageQuery,
                                                   @RequestParam(required = false) Integer status) {
        return R.ok(couponService.myCoupons(user.userId(), status, pageQuery));
    }
}
