package com.mall.pay.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.service.OrderService;
import com.mall.pay.channel.PayChannel;
import com.mall.pay.dto.PayCallbackRequest;
import com.mall.pay.dto.PayCreateRequest;
import com.mall.pay.entity.Payment;
import com.mall.pay.mapper.PaymentMapper;
import com.mall.pay.vo.PayCreateVO;
import com.mall.stock.entity.OutboundTask;
import com.mall.stock.entity.StockLock;
import com.mall.stock.mapper.OutboundTaskMapper;
import com.mall.stock.mapper.StockLockMapper;
import com.mall.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付服务。
 * <p>
 * create: 创建 pay_payment（沙箱模拟），调用 PayChannel 生成支付凭证。
 * callback: 幂等处理——第三方交易号唯一索引保证重复回调直接返回成功。
 *           成功回调：订单 10待支付 -> 20待发货，扣 wms_stock，生成 wms_outbound_task。
 */
@Slf4j
@Service
public class PayService {

    // 支付状态
    public static final int PAY_PENDING = 0;
    public static final int PAY_SUCCESS = 1;
    public static final int PAY_FAILED = 2;

    // 订单状态（按状态机）
    public static final int ORDER_PENDING_PAY = 10;
    public static final int ORDER_PENDING_SHIP = 20;
    public static final int ORDER_CANCELLED = 5;

    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final OrderItemMapper orderItemMapper;
    private final StockService stockService;
    private final StockLockMapper stockLockMapper;
    private final OutboundTaskMapper outboundTaskMapper;
    private final PayChannel payChannel;

    public PayService(PaymentMapper paymentMapper,
                      OrderService orderService,
                      OrderItemMapper orderItemMapper,
                      StockService stockService,
                      StockLockMapper stockLockMapper,
                      OutboundTaskMapper outboundTaskMapper,
                      PayChannel payChannel) {
        this.paymentMapper = paymentMapper;
        this.orderService = orderService;
        this.orderItemMapper = orderItemMapper;
        this.stockService = stockService;
        this.stockLockMapper = stockLockMapper;
        this.outboundTaskMapper = outboundTaskMapper;
        this.payChannel = payChannel;
    }

    // ==================== create ====================

    public PayCreateVO create(Long userId, PayCreateRequest req) {
        Order order = orderService.findById(req.getOrderId());

        if (!order.getMemberId().equals(userId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }

        if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_PENDING_PAY) {
            throw new MallException(ErrorCode.Biz.ORDER_STATUS_INVALID, "订单不在待付款状态");
        }

        Payment existing = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .eq(Payment::getStatus, PAY_PENDING)
                .last("LIMIT 1"));
        if (existing != null) {
            PayChannel.PayResult result = payChannel.pay(
                    existing.getTransactionNo(), existing.getPayAmount(), req.getPayMethod());
            return buildPayCreateVO(existing, result);
        }

        String transactionNo = generateTransactionNo();
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setMemberId(userId);
        payment.setPayAmount(order.getPayAmount());
        payment.setPayMethod(req.getPayMethod());
        payment.setTransactionNo(transactionNo);
        payment.setStatus(PAY_PENDING);

        try {
            paymentMapper.insert(payment);
        } catch (DuplicateKeyException e) {
            log.warn("交易号冲突，重试: {}", transactionNo);
            transactionNo = generateTransactionNo();
            payment.setTransactionNo(transactionNo);
            paymentMapper.insert(payment);
        }

        PayChannel.PayResult result = payChannel.pay(
                transactionNo, payment.getPayAmount(), req.getPayMethod());

        log.info("创建支付: paymentId={}, txNo={}, orderNo={}", payment.getId(), transactionNo, order.getOrderNo());

        return buildPayCreateVO(payment, result);
    }

    // ==================== callback ====================

    @Transactional(rollbackFor = Exception.class)
    public boolean callback(PayCallbackRequest req) {
        if (!payChannel.verifyCallback(req.getCallbackContent())) {
            log.warn("回调签名校验失败: txNo={}", req.getTransactionNo());
            return false;
        }

        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getTransactionNo, req.getTransactionNo())
                .last("LIMIT 1"));
        if (payment == null) {
            log.warn("回调交易号不存在: txNo={}", req.getTransactionNo());
            return false;
        }

        if (payment.getStatus() != null && payment.getStatus() == PAY_SUCCESS) {
            log.info("重复回调直接返回成功: txNo={}", req.getTransactionNo());
            return true;
        }

        if (req.getAmount() != null && payment.getPayAmount().compareTo(req.getAmount()) != 0) {
            log.error("回调金额不匹配: expected={}, actual={}", payment.getPayAmount(), req.getAmount());
            throw new MallException(ErrorCode.Biz.PAY_AMOUNT_MISMATCH);
        }

        if (req.getStatus() != null && req.getStatus() == PAY_FAILED) {
            payment.setStatus(PAY_FAILED);
            payment.setCallbackContent(req.getCallbackContent());
            paymentMapper.updateById(payment);
            log.info("支付失败: txNo={}", req.getTransactionNo());
            return true;
        }

        payment.setStatus(PAY_SUCCESS);
        payment.setPayTime(LocalDateTime.now());
        payment.setCallbackContent(req.getCallbackContent());
        paymentMapper.updateById(payment);

        Order order = orderService.findByOrderNo(payment.getOrderNo());
        if (order == null) {
            log.error("支付回调找不到订单: orderNo={}", payment.getOrderNo());
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }

        int fromStatus = order.getOrderStatus() == null ? -1 : order.getOrderStatus();
        order.setOrderStatus(ORDER_PENDING_SHIP);
        order.setPayStatus(1);
        order.setPayMethod(payment.getPayMethod());
        order.setPaymentTime(LocalDateTime.now());
        orderService.updateOrder(order);
        orderService.logStatus(order, fromStatus, ORDER_PENDING_SHIP, 0, null, "支付成功");

        List<StockLock> locks = stockLockMapper.selectList(Wrappers.<StockLock>lambdaQuery()
                .eq(StockLock::getOrderId, order.getId())
                .eq(StockLock::getStatus, 0));
        for (StockLock lock : locks) {
            stockService.deductFrozen(lock.getSkuId(), lock.getQuantity());
            lock.setStatus(1);
            stockLockMapper.updateById(lock);
        }

        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        int totalItems = items.stream().mapToInt(OrderItem::getQuantity).sum();

        OutboundTask task = new OutboundTask();
        task.setOrderId(order.getId());
        task.setOrderNo(order.getOrderNo());
        task.setShopId(order.getShopId());
        task.setWarehouseCode("default");
        task.setStatus(0);
        task.setTotalItems(totalItems);
        outboundTaskMapper.insert(task);

        log.info("支付回调成功: txNo={}, orderNo={}, outboundTaskTotal={}",
                req.getTransactionNo(), order.getOrderNo(), totalItems);

        return true;
    }

    // ==================== 私有方法 ====================

    private PayCreateVO buildPayCreateVO(Payment payment, PayChannel.PayResult result) {
        PayCreateVO vo = new PayCreateVO();
        vo.setPaymentId(payment.getId());
        vo.setTransactionNo(payment.getTransactionNo());
        vo.setPayUrl(result.payUrl());
        vo.setSuccess(result.success());
        vo.setMessage(result.message());
        return vo;
    }

    private String generateTransactionNo() {
        return "TX" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 8);
    }
}
