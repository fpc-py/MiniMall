package com.mall.system.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.common.security.RequirePermission;
import com.mall.system.service.AdminStatsService;
import com.mall.system.vo.ShopStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端 - 统计")
@RestController
@RequestMapping("/api/shop/stats")
@RequiredArgsConstructor
public class ShopStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "店铺统计概览", description = "本店商品数/订单数/待发货数/销售额，供商家仪表盘使用")
    @GetMapping("/overview")
    @RequirePermission("shop:stats:view")
    public R<ShopStatsVO> overview(@CurrentUser LoginUser user) {
        return R.ok(adminStatsService.shopOverview(getShopId(user)));
    }

    private Long getShopId(LoginUser user) {
        if (user == null || user.shopId() == null) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        return user.shopId();
    }
}
