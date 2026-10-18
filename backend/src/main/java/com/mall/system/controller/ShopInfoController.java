package com.mall.system.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.common.security.RequirePermission;
import com.mall.system.dto.ShopInfoUpdateRequest;
import com.mall.system.service.ShopService;
import com.mall.system.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端 - 店铺信息")
@RestController
@RequestMapping("/api/shop/info")
@RequiredArgsConstructor
public class ShopInfoController {

    private final ShopService shopService;

    @Operation(summary = "查看本店信息")
    @GetMapping
    @RequirePermission("shop:info:view")
    public R<ShopVO> info(@CurrentUser LoginUser user) {
        return R.ok(shopService.getInfo(getShopId(user)));
    }

    @Operation(summary = "修改本店信息", description = "仅可修改联系人/地址/简介等展示字段，店铺状态不可自改")
    @PutMapping
    @RequirePermission("shop:info:update")
    public R<ShopVO> update(@CurrentUser LoginUser user,
                            @Valid @RequestBody ShopInfoUpdateRequest req) {
        return R.ok(shopService.updateInfo(getShopId(user), req));
    }

    private Long getShopId(LoginUser user) {
        if (user == null || user.shopId() == null) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        return user.shopId();
    }
}
