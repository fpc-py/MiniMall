package com.mall.pay.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.order.entity.Order;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.service.OrderService;
import com.mall.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单定时任务。
 * <p>
 * 每 5 分钟扫描一次：待付款(10)且超过 30 分钟未支付的订单。
 * 释放锁库存 + 取消订单(05)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    /** 待付款 */
    private static final int ORDER_PENDING_PAY = 10;
    /** 已取消 */
    private static final int ORDER_CANCELLED = 5;
    /** 超时时间（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final StockService stockService;

    @Scheduled(fixedDelay = 300_000)
    public void cancelTimeoutOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<Order> timeoutOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderStatus, ORDER_PENDING_PAY)
                .lt(Order::getCreateTime, threshold)
                .orderByAsc(Order::getCreateTime));

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("超时关单任务: 发现 {} 笔待付款超时订单", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                cancelOrder(order);
            } catch (Exception e) {
                log.error("超时关单失败: orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    private void cancelOrder(Order order) {
        stockService.releaseByOrder(order.getId());

        int fromStatus = order.getOrderStatus() == null ? -1 : order.getOrderStatus();
        order.setOrderStatus(ORDER_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("超时未支付自动关闭");
        orderMapper.updateById(order);

        orderService.logStatus(order, fromStatus, ORDER_CANCELLED, 0, null, "超时自动关单");

        log.info("超时关单成功: orderNo={}", order.getOrderNo());
    }
}
