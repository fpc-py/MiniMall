package com.mall.order.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.RequirePermission;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.vo.OrderConfirmVO;
import com.mall.order.vo.OrderDetailVO;
import com.mall.order.vo.OrderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "平台端 - 订单管理")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Operation(summary = "全部订单列表", description = "平台管理员查看所有订单，可按状态筛选")
    @GetMapping
    @RequirePermission("admin:order:list")
    public R<PageResult<OrderListVO>> list(@Valid PageQuery pageQuery,
                                            @RequestParam(required = false) Integer orderStatus,
                                            @RequestParam(required = false) String orderNo) {
        Page<Order> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        var wrapper = Wrappers.<Order>lambdaQuery()
                .eq(orderStatus != null, Order::getOrderStatus, orderStatus)
                .like(orderNo != null, Order::getOrderNo, orderNo)
                .orderByDesc(Order::getCreateTime)
                .orderByDesc(Order::getId);
        var result = orderMapper.selectPage(page, wrapper);
        var vos = result.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos));
    }

    @Operation(summary = "订单详情（管理用）")
    @GetMapping("/{id}")
    @RequirePermission("admin:order:detail")
    public R<OrderDetailVO> detail(@PathVariable("id") Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return R.fail(400, "订单不存在");
        }
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayStatus(order.getPayStatus());
        vo.setPayMethod(order.getPayMethod());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        String addr = joinAddress(order);
        vo.setReceiverAddress(addr);
        vo.setDeliveryCompany(order.getDeliveryCompany());
        vo.setDeliveryNo(order.getDeliveryNo());
        vo.setMemberRemark(order.getMemberRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setPaymentTime(order.getPaymentTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setReceiveTime(order.getReceiveTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());

        var items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, id));
        List<OrderConfirmVO.OrderItemVO> itemVOs = items.stream().map(item -> {
            var itemVo = new OrderConfirmVO.OrderItemVO();
            itemVo.setSkuId(item.getSkuId());
            itemVo.setSpuId(item.getSpuId());
            itemVo.setProductName(item.getProductName());
            itemVo.setProductImage(item.getProductImage());
            itemVo.setAttrs(item.getAttrs());
            itemVo.setPrice(item.getPrice());
            itemVo.setQuantity(item.getQuantity());
            itemVo.setSubtotal(item.getSubtotal());
            return itemVo;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);
        return R.ok(vo);
    }

    private OrderListVO toListVO(Order order) {
        OrderListVO vo = new OrderListVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayStatus(order.getPayStatus());
        vo.setPayAmount(order.getPayAmount());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setCreateTime(order.getCreateTime());
        vo.setPaymentTime(order.getPaymentTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        return vo;
    }

    private String joinAddress(Order o) {
        StringBuilder sb = new StringBuilder();
        if (o.getReceiverProvince() != null) sb.append(o.getReceiverProvince());
        if (o.getReceiverCity() != null) sb.append(o.getReceiverCity());
        if (o.getReceiverDistrict() != null) sb.append(o.getReceiverDistrict());
        if (o.getReceiverAddress() != null) sb.append(" ").append(o.getReceiverAddress());
        return sb.toString();
    }
}
