package com.mall.ai.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 订单查询工具。所有查询强制以 JWT 中的 memberId 做归属过滤，查询不到他人订单时按「未查询到」处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAgentTools {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<Integer, String> ORDER_STATUS_TEXT = Map.of(
            0, "待付款", 1, "待发货", 2, "已发货", 3, "已收货", 4, "已完成", 5, "已取消", 6, "售后中");

    private static final String ORDER_NOT_FOUND =
            "未查询到该订单。请核对订单号是否准确；若确认无误，可回复「转人工」由人工客服为您核实。";

    /** 未指定订单号时最多列出的订单数 */
    private static final int RECENT_LIMIT = 5;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Tool(name = "query_order",
            description = "查询当前登录用户的订单。用户给出订单号时查该订单详情；未给出订单号时按状态列出最近订单。",
            resultConverter = AiTextResultConverter.class)
    public String queryOrder(
            @ToolParam(required = false, description = "订单号，22 位纯数字；用户明确给出订单号时传入") String orderNo,
            @ToolParam(required = false,
                    description = "订单状态筛选，可选：0待付款 1待发货 2已发货 3已收货 4已完成 5已取消 6售后中")
            Integer status,
            ToolContext toolContext) {
        Long memberId = AiToolSupport.userId(toolContext);
        if (memberId == null) {
            return "请先登录后再查询订单。";
        }
        AiProgressNotifier notifier = AiToolSupport.notifier(toolContext);
        notifier.tool("query_order", "running");
        try {
            if (orderNo != null && !orderNo.isBlank()) {
                return queryByOrderNo(orderNo.trim(), memberId);
            }
            return queryRecent(memberId, status);
        } catch (Exception e) {
            log.warn("query_order 执行异常: {}", e.getMessage());
            return "抱歉，订单查询出了点小问题，请稍后再试。";
        } finally {
            notifier.tool("query_order", "done");
        }
    }

    private String queryByOrderNo(String orderNo, Long memberId) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getMemberId, memberId));
        if (order == null) {
            return ORDER_NOT_FOUND;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("订单号：").append(order.getOrderNo()).append('\n');
        builder.append("订单状态：").append(statusText(order.getOrderStatus())).append('\n');
        builder.append("实付金额：").append(AiToolSupport.money(order.getPayAmount())).append(" 元\n");
        if (order.getCreateTime() != null) {
            builder.append("下单时间：").append(order.getCreateTime().format(TIME_FORMATTER)).append('\n');
        }
        String items = itemSummary(order.getId());
        if (!items.isEmpty()) {
            builder.append("商品：").append(items).append('\n');
        }
        if (order.getDeliveryCompany() != null || order.getDeliveryNo() != null) {
            builder.append("物流：").append(nullToEmpty(order.getDeliveryCompany()))
                    .append(' ').append(nullToEmpty(order.getDeliveryNo())).append('\n');
        }
        return builder.toString().trim();
    }

    private String queryRecent(Long memberId, Integer status) {
        List<Order> orders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getMemberId, memberId)
                .eq(status != null, Order::getOrderStatus, status)
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT " + RECENT_LIMIT));
        if (orders.isEmpty()) {
            return status == null ? "您目前还没有订单记录。"
                    : "您目前没有「" + statusText(status) + "」状态的订单。";
        }
        StringBuilder builder = new StringBuilder("为您查询到最近的订单：\n");
        int index = 1;
        for (Order order : orders) {
            builder.append(index++).append(". 订单号 ").append(order.getOrderNo())
                    .append(" ｜ ").append(statusText(order.getOrderStatus()))
                    .append(" ｜ 实付 ").append(AiToolSupport.money(order.getPayAmount())).append(" 元");
            if (order.getCreateTime() != null) {
                builder.append(" ｜ ").append(order.getCreateTime().format(TIME_FORMATTER));
            }
            builder.append('\n');
        }
        builder.append("如需查看某一单的详情，请把订单号发给我。");
        return builder.toString();
    }

    private String itemSummary(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            if (i > 0) {
                builder.append("；");
            }
            builder.append(item.getProductName()).append(" × ").append(item.getQuantity());
        }
        return builder.toString();
    }

    private String statusText(Integer status) {
        return status == null ? "未知" : ORDER_STATUS_TEXT.getOrDefault(status, "未知");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
