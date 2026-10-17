package com.mall.order.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.order.dto.RefundApplyRequest;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.entity.Refund;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.RefundMapper;
import com.mall.marketing.service.CouponService;
import com.mall.order.vo.RefundVO;
import com.mall.stock.service.StockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退款服务测试：归属校验、重复申请拦截、退款执行（回补库存 + 订单支付状态）+ 状态机幂等。
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long SHOP_ID = 9L;

    @Mock
    private RefundMapper refundMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private StockService stockService;
    @Mock
    private CouponService couponService;

    private RefundService refundService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Refund.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
    }

    @BeforeEach
    void setUp() {
        refundService = new RefundService(refundMapper, orderMapper, orderItemMapper, stockService, couponService);
    }

    private Order paidOrder() {
        Order order = new Order();
        order.setId(100L);
        order.setOrderNo("OD202601010001");
        order.setMemberId(MEMBER_ID);
        order.setShopId(SHOP_ID);
        order.setPayAmount(new BigDecimal("199.00"));
        order.setPayStatus(OrderService.PAY_PAID);
        order.setOrderStatus(OrderService.STATUS_PENDING_SHIP);
        return order;
    }

    private Refund pendingRefund() {
        Refund refund = new Refund();
        refund.setId(500L);
        refund.setRefundNo("R20260101000000123");
        refund.setOrderId(100L);
        refund.setOrderNo("OD202601010001");
        refund.setMemberId(MEMBER_ID);
        refund.setShopId(SHOP_ID);
        refund.setRefundAmount(new BigDecimal("199.00"));
        refund.setStatus(RefundService.STATUS_PENDING);
        return refund;
    }

    private RefundApplyRequest applyRequest() {
        RefundApplyRequest req = new RefundApplyRequest();
        req.setOrderId(100L);
        req.setReason("不想要了");
        req.setImages(List.of("http://img/a.jpg"));
        return req;
    }

    @Test
    @DisplayName("申请：订单不存在抛 30000")
    void applyOrderNotFound() {
        when(orderMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> refundService.apply(MEMBER_ID, applyRequest()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.ORDER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("申请：他人订单抛 20001（越权）")
    void applyRejectsOtherMemberOrder() {
        Order order = paidOrder();
        order.setMemberId(999L);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertThatThrownBy(() -> refundService.apply(MEMBER_ID, applyRequest()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());

        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    @DisplayName("申请：未支付订单抛 30028")
    void applyRejectsUnpaidOrder() {
        Order order = paidOrder();
        order.setPayStatus(OrderService.PAY_UNPAID);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertThatThrownBy(() -> refundService.apply(MEMBER_ID, applyRequest()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.ORDER_NOT_REFUNDABLE.getCode());
    }

    @Test
    @DisplayName("申请：已有处理中的退款单抛 30026")
    void applyRejectsDuplicateOpenRefund() {
        when(orderMapper.selectById(100L)).thenReturn(paidOrder());
        when(refundMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> refundService.apply(MEMBER_ID, applyRequest()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.REFUND_ALREADY_APPLIED.getCode());

        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    @DisplayName("申请：正常写入待审核退款单，金额取订单实付")
    void applyCreatesPendingRefund() {
        when(orderMapper.selectById(100L)).thenReturn(paidOrder());
        when(refundMapper.selectCount(any())).thenReturn(0L);
        when(refundMapper.insert(any(Refund.class))).thenAnswer(inv -> {
            inv.getArgument(0, Refund.class).setId(500L);
            return 1;
        });

        RefundVO vo = refundService.apply(MEMBER_ID, applyRequest());

        assertThat(vo.getStatus()).isEqualTo(RefundService.STATUS_PENDING);
        assertThat(vo.getRefundAmount()).isEqualByComparingTo("199.00");
        assertThat(vo.getRefundNo()).startsWith("R");
        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundMapper).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().getShopId()).isEqualTo(SHOP_ID);
        assertThat(captor.getValue().getImages()).isEqualTo("http://img/a.jpg");
    }

    @Test
    @DisplayName("撤销：非本人退款单抛 20001")
    void cancelRejectsOtherMember() {
        Refund refund = pendingRefund();
        refund.setMemberId(999L);
        when(refundMapper.selectById(500L)).thenReturn(refund);

        assertThatThrownBy(() -> refundService.cancel(MEMBER_ID, 500L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("撤销：已处理的退款单抛 30027")
    void cancelRejectsHandledRefund() {
        Refund refund = pendingRefund();
        refund.setStatus(RefundService.STATUS_REFUNDED);
        when(refundMapper.selectById(500L)).thenReturn(refund);

        assertThatThrownBy(() -> refundService.cancel(MEMBER_ID, 500L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.REFUND_STATUS_INVALID.getCode());
    }

    @Test
    @DisplayName("撤销：正常置为已撤销")
    void cancelMarksCancelled() {
        when(refundMapper.selectById(500L)).thenReturn(pendingRefund());

        refundService.cancel(MEMBER_ID, 500L);

        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RefundService.STATUS_CANCELLED);
    }

    @Test
    @DisplayName("审核：非本店退款单抛 20001")
    void auditRejectsOtherShop() {
        when(refundMapper.selectById(500L)).thenReturn(pendingRefund());

        assertThatThrownBy(() -> refundService.audit(888L, 500L, RefundService.STATUS_REFUNDING, null, 7L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("审核：已处理的退款单抛 30027（幂等，不重复退款）")
    void auditRejectsHandledRefund() {
        Refund refund = pendingRefund();
        refund.setStatus(RefundService.STATUS_REFUNDED);
        when(refundMapper.selectById(500L)).thenReturn(refund);

        assertThatThrownBy(() -> refundService.audit(SHOP_ID, 500L, RefundService.STATUS_REFUNDING, null, 7L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.REFUND_STATUS_INVALID.getCode());

        verify(stockService, never()).returnStock(any(), any(Integer.class));
    }

    @Test
    @DisplayName("审核同意：回补每个订单项库存 + 订单支付状态置为已退款 + 退款单置为已退款")
    void auditApproveExecutesRefund() {
        when(refundMapper.selectById(500L)).thenReturn(pendingRefund());
        OrderItem item1 = new OrderItem();
        item1.setOrderId(100L);
        item1.setSkuId(11L);
        item1.setQuantity(2);
        OrderItem item2 = new OrderItem();
        item2.setOrderId(100L);
        item2.setSkuId(12L);
        item2.setQuantity(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item1, item2));
        when(orderMapper.selectById(100L)).thenReturn(paidOrder());

        refundService.audit(SHOP_ID, 500L, RefundService.STATUS_REFUNDING, "同意退款", 7L);

        verify(stockService).returnStock(11L, 2);
        verify(stockService).returnStock(12L, 1);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getPayStatus()).isEqualTo(2);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundMapper).updateById(refundCaptor.capture());
        Refund saved = refundCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RefundService.STATUS_REFUNDED);
        assertThat(saved.getRefundTime()).isNotNull();
        assertThat(saved.getAuditTime()).isNotNull();
        assertThat(saved.getTransactionNo()).startsWith("SANDBOX-REFUND-");
    }

    @Test
    @DisplayName("审核拒绝：仅回写状态与备注，不回补库存、不改订单")
    void auditReject() {
        when(refundMapper.selectById(500L)).thenReturn(pendingRefund());

        refundService.audit(SHOP_ID, 500L, RefundService.STATUS_REJECTED, "不符合退款条件", 7L);

        verify(stockService, never()).returnStock(any(), any(Integer.class));
        verify(orderMapper, never()).updateById(any(Order.class));
        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RefundService.STATUS_REJECTED);
        assertThat(captor.getValue().getAuditRemark()).isEqualTo("不符合退款条件");
    }

    @Test
    @DisplayName("审核：非法结果值抛 10000")
    void auditRejectsIllegalStatus() {
        when(refundMapper.selectById(500L)).thenReturn(pendingRefund());

        assertThatThrownBy(() -> refundService.audit(SHOP_ID, 500L, 9, null, 7L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());

        verify(refundMapper, never()).updateById(any(Refund.class));
    }
}
