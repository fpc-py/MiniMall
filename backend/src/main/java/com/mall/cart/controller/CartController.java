package com.mall.cart.controller;

import com.mall.cart.dto.CartAddRequest;
import com.mall.cart.dto.CartUpdateRequest;
import com.mall.cart.service.CartService;
import com.mall.cart.vo.CartVO;
import com.mall.cart.vo.CartSummaryVO;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "购物车")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "加购", description = "已存在同 SKU 则累加数量")
    @PostMapping("/add")
    public R<Void> add(@CurrentUser LoginUser user, @Valid @RequestBody CartAddRequest req) {
        cartService.addItem(user.userId(), req.getSkuId(), req.getQuantity());
        return R.ok();
    }

    @Operation(summary = "修改购物车项", description = "修改数量 / 选中状态")
    @PutMapping("/items/{skuId}")
    public R<Void> update(@CurrentUser LoginUser user,
                          @PathVariable("skuId") Long skuId,
                          @RequestBody CartUpdateRequest req) {
        cartService.updateItem(user.userId(), skuId, req.getQuantity(), req.getSelected());
        return R.ok();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/items/{skuId}")
    public R<Void> remove(@CurrentUser LoginUser user, @PathVariable("skuId") Long skuId) {
        cartService.removeItem(user.userId(), skuId);
        return R.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public R<Void> clear(@CurrentUser LoginUser user) {
        cartService.clear(user.userId());
        return R.ok();
    }

    @Operation(summary = "购物车列表", description = "按店铺分组，含实时可售库存与合计")
    @GetMapping
    public R<CartVO> getCart(@CurrentUser LoginUser user) {
        return R.ok(cartService.getCart(user.userId()));
    }

    @Operation(summary = "全选 / 全不选")
    @PutMapping("/select-all")
    public R<Void> selectAll(@CurrentUser LoginUser user,
                            @RequestParam(value = "selected", defaultValue = "true") boolean selected) {
        cartService.selectAll(user.userId(), selected);
        return R.ok();
    }

    @Operation(summary = "购物车合计", description = "选中商品件数 / 总金额")
    @GetMapping("/summary")
    public R<CartSummaryVO> summary(@CurrentUser LoginUser user) {
        return R.ok(cartService.summary(user.userId()));
    }
}
