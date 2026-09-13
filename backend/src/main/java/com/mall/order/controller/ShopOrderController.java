package com.mall.order.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.common.security.RequirePermission;
import com.mall.order.dto.OrderShipRequest;
import com.mall.order.service.OrderService;
import com.mall.order.vo.OrderDetailVO;
import com.mall.order.vo.OrderListVO;
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

@Tag(name = "商家端 - 订单管理")
@RestController
@RequestMapping("/api/shop/orders")
@RequiredArgsConstructor
public class ShopOrderController {

    private final OrderService orderService;

    @Operation(summary = "店铺订单列表", description = "商家查看自己店铺的订单，可按状态筛选")
    @GetMapping
    @RequirePermission("shop:order:list")
    public R<PageResult<OrderListVO>> list(@CurrentUser LoginUser user,
                                           @Valid PageQuery pageQuery,
                                           @RequestParam(required = false) Integer orderStatus) {
        Long shopId = getShopId(user);
        return R.ok(orderService.shopOrders(shopId, pageQuery, orderStatus));
    }

    @Operation(summary = "店铺订单详情")
    @GetMapping("/{id}")
    @RequirePermission("shop:order:detail")
    public R<OrderDetailVO> detail(@CurrentUser LoginUser user,
                                   @PathVariable("id") Long id) {
        Long shopId = getShopId(user);
        return R.ok(orderService.shopDetail(shopId, id));
    }

    @Operation(summary = "商家发货", description = "待发货(20) -> 待收货(30)，记录快递信息")
    @PostMapping("/ship")
    @RequirePermission("shop:order:ship")
    public R<Void> ship(@CurrentUser LoginUser user,
                        @Valid @RequestBody OrderShipRequest req) {
        Long shopId = getShopId(user);
        orderService.ship(shopId, req.getOrderId(), req.getDeliveryCompany(), req.getDeliveryNo());
        return R.ok();
    }

    /** 从登录用户中获取 shop_id */
    private Long getShopId(LoginUser user) {
        if (user == null || user.shopId() == null) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        return user.shopId();
    }
}
