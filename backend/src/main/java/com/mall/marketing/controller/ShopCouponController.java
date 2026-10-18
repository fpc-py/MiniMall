package com.mall.marketing.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
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

@Tag(name = "商家端 - 店铺优惠券")
@RestController
@RequestMapping("/api/shop/coupons")
@RequiredArgsConstructor
public class ShopCouponController {

    private final CouponService couponService;

    @Operation(summary = "店铺优惠券列表", description = "商家查看本店优惠券，可按名称与状态筛选")
    @GetMapping
    @RequirePermission("shop:coupon:list")
    public R<PageResult<CouponVO>> page(@CurrentUser LoginUser user,
                                        @Valid PageQuery pageQuery,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(couponService.pageForShop(getShopId(user), keyword, status, pageQuery));
    }

    @Operation(summary = "创建店铺优惠券", description = "shop_id 取自当前登录商家，仅限本店使用")
    @PostMapping
    @RequirePermission("shop:coupon:create")
    public R<CouponVO> create(@CurrentUser LoginUser user,
                              @Valid @RequestBody CouponSaveRequest req) {
        return R.ok(couponService.createForShop(getShopId(user), req));
    }

    @Operation(summary = "启用/停用店铺优惠券", description = "status 0停用 1启用；非本店券返回无权限")
    @PostMapping("/{id}/status")
    @RequirePermission("shop:coupon:status")
    public R<Void> updateStatus(@CurrentUser LoginUser user,
                                @PathVariable("id") Long id,
                                @RequestParam Integer status) {
        couponService.updateStatusForShop(getShopId(user), id, status);
        return R.ok();
    }

    private Long getShopId(LoginUser user) {
        if (user == null || user.shopId() == null) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        return user.shopId();
    }
}
