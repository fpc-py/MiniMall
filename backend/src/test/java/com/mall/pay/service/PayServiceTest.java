package com.mall.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付测试重点：幂等回调、金额校验、库存扣减、出库任务生成。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayServiceTest {

    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderService orderService;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private StockService stockService;
    @Mock private StockLockMapper stockLockMapper;
    @Mock private OutboundTaskMapper outboundTaskMapper;
    @Mock private PayChannel payChannel;

    private PayService payService;

    @BeforeEach
    void setUp() {
        payService = new PayService(paymentMapper, orderService, orderItemMapper,
                stockService, stockLockMapper, outboundTaskMapper, payChannel);
    }

    private Order order(long id, long memberId, int status, BigDecimal payAmount) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("NO" + id);
        order.setMemberId(memberId);
        order.setShopId(100L);
        order.setOrderStatus(status);
        order.setPayStatus(0);
        order.setPayAmount(payAmount);
        return order;
    }

    private Payment payment(long id, long orderId, String txNo, int status, BigDecimal amount) {
        Payment p = new Payment();
        p.setId(id);
        p.setOrderId(orderId);
        p.setOrderNo("NO" + orderId);
        p.setMemberId(1L);
        p.setTransactionNo(txNo);
        p.setStatus(status);
        p.setPayAmount(amount);
        p.setPayMethod(2);
        return p;
    }

    // ==================== create ====================

    @Test
    @DisplayName("发起支付：正常创建支付记录并返回沙箱支付 URL")
    void createPaymentSuccessfully() {
        Order order = order(1L, 1L, PayService.ORDER_PENDING_PAY, new BigDecimal("5999.00"));
        when(orderService.findById(1L)).thenReturn(order);
        when(paymentMapper.selectOne(any())).thenReturn(null); // 无已存在记录
        PayChannel.PayResult result = new PayChannel.PayResult("TX001", "http://sandbox/pay?tx=TX001", true, "OK");
        when(payChannel.pay(anyString(), any(BigDecimal.class), eq(2))).thenReturn(result);

        PayCreateRequest req = new PayCreateRequest();
        req.setOrderId(1L);
        req.setPayMethod(2);

        PayCreateVO vo = payService.create(1L, req);

        assertThat(vo.getTransactionNo()).isNotBlank();
        assertThat(vo.getPayUrl()).contains("sandbox");
        assertThat(vo.getSuccess()).isTrue();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(PayService.PAY_PENDING);
        assertThat(saved.getPayAmount()).isEqualByComparingTo("5999.00");
    }

    @Test
    @DisplayName("发起支付：越权操作他人订单抛 20001")
    void createThrowsWhenNotOwner() {
        Order order = order(1L, 999L, PayService.ORDER_PENDING_PAY, new BigDecimal("99.00"));
        when(orderService.findById(1L)).thenReturn(order);

        PayCreateRequest req = new PayCreateRequest();
        req.setOrderId(1L);
        req.setPayMethod(2);

        assertThatThrownBy(() -> payService.create(1L, req))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("发起支付：订单非待付款状态抛 30004")
    void createThrowsWhenOrderNotPendingPay() {
        Order order = order(1L, 1L, PayService.ORDER_PENDING_SHIP, new BigDecimal("99.00"));
        when(orderService.findById(1L)).thenReturn(order);

        PayCreateRequest req = new PayCreateRequest();
        req.setOrderId(1L);
        req.setPayMethod(2);

        assertThatThrownBy(() -> payService.create(1L, req))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.ORDER_STATUS_INVALID.getCode());
    }

    @Test
    @DisplayName("发起支付：已存在待支付记录则复用")
    void createReusesExistingPendingPayment() {
        Order order = order(1L, 1L, PayService.ORDER_PENDING_PAY, new BigDecimal("99.00"));
        Payment existing = payment(10L, 1L, "TX_EXISTING", PayService.PAY_PENDING, new BigDecimal("99.00"));
        when(orderService.findById(1L)).thenReturn(order);
        when(paymentMapper.selectOne(any())).thenReturn(existing);
        PayChannel.PayResult result = new PayChannel.PayResult("TX_EXISTING", "http://sandbox/pay", true, "OK");
        when(payChannel.pay(eq("TX_EXISTING"), any(BigDecimal.class), eq(2))).thenReturn(result);

        PayCreateRequest req = new PayCreateRequest();
        req.setOrderId(1L);
        req.setPayMethod(2);

        PayCreateVO vo = payService.create(1L, req);

        assertThat(vo.getTransactionNo()).isEqualTo("TX_EXISTING");
        // 不应再 insert 新记录
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    // ==================== callback ====================

    @Test
    @DisplayName("回调：正常成功——更新订单为待发货 + 扣库存 + 生成出库任务")
    void callbackSuccessUpdatesOrderAndStock() {
        String txNo = "TX001";
        Payment payment = payment(10L, 1L, txNo, PayService.PAY_PENDING, new BigDecimal("5999.00"));
        Order order = order(1L, 1L, PayService.ORDER_PENDING_PAY, new BigDecimal("5999.00"));

        StockLock lock = new StockLock();
        lock.setId(1L);
        lock.setSkuId(100L);
        lock.setOrderId(1L);
        lock.setQuantity(2);
        lock.setStatus(0);

        OrderItem item = new OrderItem();
        item.setOrderId(1L);
        item.setQuantity(2);

        when(payChannel.verifyCallback(anyString())).thenReturn(true);
        when(paymentMapper.selectOne(any())).thenReturn(payment);
        when(orderService.findByOrderNo("NO1")).thenReturn(order);
        when(stockLockMapper.selectList(any())).thenReturn(List.of(lock));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        PayCallbackRequest req = new PayCallbackRequest();
        req.setTransactionNo(txNo);
        req.setStatus(1);
        req.setCallbackContent("{\"status\":\"success\"}");

        boolean result = payService.callback(req);

        assertThat(result).isTrue();

        // 验证支付记录更新为成功
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).updateById(paymentCaptor.capture());
        Payment updated = paymentCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo(PayService.PAY_SUCCESS);
        assertThat(updated.getPayTime()).isNotNull();

        // 验证订单更新为待发货
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).updateOrder(orderCaptor.capture());
        Order updatedOrder = orderCaptor.getValue();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(PayService.ORDER_PENDING_SHIP);
        assertThat(updatedOrder.getPayStatus()).isEqualTo(1);

        // 验证库存扣减
        verify(stockService).deductFrozen(100L, 2);

        // 验证出库任务创建
        ArgumentCaptor<OutboundTask> taskCaptor = ArgumentCaptor.forClass(OutboundTask.class);
        verify(outboundTaskMapper).insert(taskCaptor.capture());
        OutboundTask task = taskCaptor.getValue();
        assertThat(task.getOrderId()).isEqualTo(1L);
        assertThat(task.getStatus()).isEqualTo(0);
        assertThat(task.getTotalItems()).isEqualTo(2);
    }

    @Test
    @DisplayName("回调：幂等——重复回调直接返回成功，不重复扣库存/改状态")
    void callbackIdempotentDuplicateReturnsSuccess() {
        String txNo = "TX001";
        Payment payment = payment(10L, 1L, txNo, PayService.PAY_SUCCESS, new BigDecimal("5999.00"));
        when(payChannel.verifyCallback(anyString())).thenReturn(true);
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        PayCallbackRequest req = new PayCallbackRequest();
        req.setTransactionNo(txNo);
        req.setStatus(1);
        req.setCallbackContent("{\"status\":\"success\"}");

        boolean result = payService.callback(req);

        assertThat(result).isTrue();

        // 不应更新支付记录
        verify(paymentMapper, never()).updateById(any(Payment.class));
        // 不应扣减库存
        verify(stockService, never()).deductFrozen(anyLong(), any(Integer.class));
        // 不应生成出库任务
        verify(outboundTaskMapper, never()).insert(any(OutboundTask.class));
        // 不应更新订单
        verify(orderService, never()).updateOrder(any());
    }

    @Test
    @DisplayName("回调：金额不匹配抛 30009")
    void callbackThrowsWhenAmountMismatch() {
        String txNo = "TX001";
        Payment payment = payment(10L, 1L, txNo, PayService.PAY_PENDING, new BigDecimal("5999.00"));
        when(payChannel.verifyCallback(anyString())).thenReturn(true);
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        PayCallbackRequest req = new PayCallbackRequest();
        req.setTransactionNo(txNo);
        req.setStatus(1);
        req.setAmount(new BigDecimal("99.00")); // 不匹配
        req.setCallbackContent("{}");

        assertThatThrownBy(() -> payService.callback(req))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.PAY_AMOUNT_MISMATCH.getCode());

        verify(stockService, never()).deductFrozen(anyLong(), any(Integer.class));
        verify(outboundTaskMapper, never()).insert(any(OutboundTask.class));
    }

    @Test
    @DisplayName("回调：交易号不存在返回 false")
    void callbackReturnsFalseWhenTxNoNotFound() {
        when(payChannel.verifyCallback(anyString())).thenReturn(true);
        when(paymentMapper.selectOne(any())).thenReturn(null);

        PayCallbackRequest req = new PayCallbackRequest();
        req.setTransactionNo("NOT_EXIST");
        req.setStatus(1);
        req.setCallbackContent("{}");

        boolean result = payService.callback(req);

        assertThat(result).isFalse();
        verify(stockService, never()).deductFrozen(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("回调：签名校验失败返回 false")
    void callbackReturnsFalseWhenSignatureInvalid() {
        when(payChannel.verifyCallback(anyString())).thenReturn(false);

        PayCallbackRequest req = new PayCallbackRequest();
        req.setTransactionNo("TX001");
        req.setStatus(1);
        req.setCallbackContent("tampered");

        boolean result = payService.callback(req);

        assertThat(result).isFalse();
        verify(paymentMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("回调：支付失败——更新状态为失败，不扣库存")
    void callbackPaymentFailedDoesNotDeductStock() {
        String txNo = "TX001";
        Payment payment = payment(10L, 1L, txNo, PayService.PAY_PENDING, new BigDecimal("5999.00"));
        when(payChannel.verifyCallback(anyString())).thenReturn(true);
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        PayCallbackRequest req = new PayCallbackRequest();
        req.setTransactionNo(txNo);
        req.setStatus(2); // 失败
        req.setCallbackContent("{\"status\":\"fail\"}");

        boolean result = payService.callback(req);

        assertThat(result).isTrue();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PayService.PAY_FAILED);

        // 不应扣库存、不应建出库
        verify(stockService, never()).deductFrozen(anyLong(), any(Integer.class));
        verify(outboundTaskMapper, never()).insert(any(OutboundTask.class));
    }
}

