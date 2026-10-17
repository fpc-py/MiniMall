package com.mall.order.controller;

import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.order.dto.OrderConfirmRequest;
import com.mall.order.dto.OrderSubmitRequest;
import com.mall.order.service.OrderService;
import com.mall.order.vo.OrderConfirmVO;
import com.mall.order.vo.OrderDetailVO;
import com.mall.order.vo.OrderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "买家端 - 订单")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单确认（试算价格）", description = "不锁库存不建订单，返回试算结果和 confirmToken")
    @PostMapping("/confirm")
    public R<OrderConfirmVO> confirm(@CurrentUser LoginUser user,
                                     @Valid @RequestBody OrderConfirmRequest req) {
        boolean fromCart = req.getFromCart() == null || req.getFromCart();
        OrderConfirmVO vo = orderService.confirm(
                user.userId(), req.getAddressId(), req.getSkuId(), req.getQuantity(), fromCart,
                req.getRemark(), req.getCouponMemberId());
        return R.ok(vo);
    }

    @Operation(summary = "下单提交", description = "在同一事务内锁库存 + 建订单 + 建订单项 + 建库存锁")
    @PostMapping("/submit")
    public R<OrderDetailVO> submit(@CurrentUser LoginUser user,
                                   @Valid @RequestBody OrderSubmitRequest req) {
        boolean fromCart = req.getFromCart() == null || req.getFromCart();
        OrderDetailVO vo = orderService.submit(
                user.userId(), req.getAddressId(), req.getSkuId(), req.getQuantity(),
                fromCart, req.getRemark(), req.getConfirmToken(), req.getCouponMemberId());
        return R.ok(vo);
    }

    @Operation(summary = "我的订单列表", description = "分页查询当前买家订单，可按状态筛选")
    @GetMapping
    public R<PageResult<OrderListVO>> myOrders(@CurrentUser LoginUser user,
                                                @Valid PageQuery pageQuery,
                                                @RequestParam(required = false) Integer orderStatus) {
        return R.ok(orderService.myOrders(user.userId(), pageQuery, orderStatus));
    }

    @Operation(summary = "订单详情", description = "校验资源归属：买家不能查看他人订单")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@CurrentUser LoginUser user,
                                   @PathVariable("id") Long id) {
        return R.ok(orderService.detail(user.userId(), id));
    }

    @Operation(summary = "取消订单", description = "买家取消待付款订单，释放锁库存")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@CurrentUser LoginUser user,
                          @PathVariable("id") Long id,
                          @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.get("cancelReason") : null;
        orderService.cancel(user.userId(), id, reason);
        return R.ok();
    }

    @Operation(summary = "确认收货", description = "买家确认收货，待收货(30) -> 已完成(40)")
    @PostMapping("/{id}/receive")
    public R<Void> receive(@CurrentUser LoginUser user,
                           @PathVariable("id") Long id) {
        orderService.receive(user.userId(), id);
        return R.ok();
    }
}

