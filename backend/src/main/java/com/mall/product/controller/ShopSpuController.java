package com.mall.product.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.common.security.RequirePermission;
import com.mall.product.dto.SpuStatusRequest;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SpuMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端 - 商品管理")
@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ShopSpuController {

    private final SpuMapper spuMapper;

    @Operation(summary = "商品上架/下架", description = "商家管理自己店铺的商品上下架状态")
    @PostMapping("/status")
    @RequirePermission("shop:product:status")
    public R<Void> updateStatus(@CurrentUser LoginUser user,
                                @Valid @RequestBody SpuStatusRequest req) {
        Long shopId = getShopId(user);
        Spu spu = spuMapper.selectById(req.getSpuId());
        if (spu == null) {
            throw new MallException(com.mall.common.ErrorCode.Biz.PRODUCT_NOT_FOUND);
        }
        if (!spu.getShopId().equals(shopId)) {
            throw new MallException(com.mall.common.ErrorCode.Auth.FORBIDDEN);
        }
        spu.setStatus(req.getStatus());
        spuMapper.updateById(spu);
        return R.ok();
    }

    private Long getShopId(LoginUser user) {
        if (user == null || user.shopId() == null) {
            throw new MallException(com.mall.common.ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        return user.shopId();
    }
}
