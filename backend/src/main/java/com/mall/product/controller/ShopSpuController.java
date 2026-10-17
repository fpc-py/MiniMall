package com.mall.product.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.common.security.RequirePermission;
import com.mall.product.dto.SpuSaveRequest;
import com.mall.product.dto.SpuStatusRequest;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.service.SpuService;
import com.mall.product.vo.SpuListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端 - 商品管理")
@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ShopSpuController {

    private final SpuMapper spuMapper;
    private final SpuService spuService;

    @Operation(summary = "发布商品", description = "商家创建 SPU/SKU，shopId 取自 Token；同时初始化 wms_stock 库存")
    @PostMapping
    @RequirePermission("shop:product:create")
    public R<Long> create(@CurrentUser LoginUser user,
                          @Valid @RequestBody SpuSaveRequest req) {
        Long shopId = getShopId(user);
        return R.ok(spuService.create(shopId, req));
    }

    @Operation(summary = "我的商品列表", description = "商家查看本店铺全部商品（含下架）")
    @GetMapping
    @RequirePermission("shop:product:list")
    public R<PageResult<SpuListItemVO>> list(@CurrentUser LoginUser user,
                                             @Valid PageQuery pageQuery,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(spuService.pageForShop(getShopId(user), keyword, pageQuery));
    }

    @Operation(summary = "商品上架/下架", description = "商家管理自己店铺的商品上下架状态")
    @PostMapping("/status")
    @RequirePermission("shop:product:status")
    public R<Void> updateStatus(@CurrentUser LoginUser user,
                                @Valid @RequestBody SpuStatusRequest req) {
        Long shopId = getShopId(user);
        Spu spu = spuMapper.selectById(req.getSpuId());
        if (spu == null) {
            throw new MallException(ErrorCode.Biz.PRODUCT_NOT_FOUND);
        }
        if (!spu.getShopId().equals(shopId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        spu.setStatus(req.getStatus());
        spuMapper.updateById(spu);
        return R.ok();
    }

    private Long getShopId(LoginUser user) {
        if (user == null || user.shopId() == null) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        return user.shopId();
    }
}
