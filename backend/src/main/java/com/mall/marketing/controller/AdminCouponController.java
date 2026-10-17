package com.mall.marketing.controller;

import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.RequirePermission;
import com.mall.marketing.dto.CouponSaveRequest;
import com.mall.marketing.service.CouponService;
import com.mall.marketing.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台端 - 优惠券管理")
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @Operation(summary = "创建优惠券", description = "平台创建优惠券，默认启用")
    @PostMapping
    @RequirePermission("admin:coupon:create")
    public R<CouponVO> create(@Valid @RequestBody CouponSaveRequest req) {
        return R.ok(couponService.create(req));
    }

    @Operation(summary = "优惠券分页", description = "平台查看全部优惠券，可按名称与状态筛选")
    @GetMapping
    @RequirePermission("admin:coupon:list")
    public R<PageResult<CouponVO>> page(@Valid PageQuery pageQuery,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(couponService.page(keyword, status, pageQuery));
    }

    @Operation(summary = "启用/停用优惠券", description = "status 0停用 1启用")
    @PostMapping("/{id}/status")
    @RequirePermission("admin:coupon:update")
    public R<Void> updateStatus(@PathVariable("id") Long id,
                                @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return R.ok();
    }
}
