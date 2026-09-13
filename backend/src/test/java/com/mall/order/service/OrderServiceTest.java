package com.mall.order.service;

import com.mall.cart.service.CartService;
import com.mall.cart.vo.CartItemVO;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.entity.OrderStatusLog;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.OrderStatusLogMapper;
import com.mall.order.vo.OrderConfirmVO;
import com.mall.order.vo.OrderDetailVO;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.stock.service.StockService;
import com.mall.system.entity.Address;
import com.mall.system.mapper.AddressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 下单测试重点：锁库存顺序、金额计算精度、confirmToken 消费、越权校验。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderStatusLogMapper orderStatusLogMapper;
    @Mock private SkuMapper skuMapper;
    @Mock private SpuMapper spuMapper;
    @Mock private AddressMapper addressMapper;
    @Mock private StockService stockService;
    @Mock private CartService cartService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderMapper, orderItemMapper, orderStatusLogMapper,
                skuMapper, spuMapper, addressMapper, stockService, cartService, redisTemplate);
    }

    private Sku sku(long id, BigDecimal price) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setSpuId(1L);
        sku.setSkuCode("SKU-" + id);
        sku.setName("黑色 256G");
        sku.setPrice(price);
        sku.setImage("http://img/" + id + ".jpg");
        sku.setAttrs("{\"颜色\":\"黑色\"}");
        sku.setStatus(1);
        sku.setSort(1);
        return sku;
    }

    private Spu spu(long id) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setName("iPhone 15");
        spu.setShopId(100L);
        spu.setStatus(1);
        spu.setAuditStatus(1);
        spu.setMainImage("http://img/iphone.jpg");
        return spu;
    }

    private Address address(long id, long memberId) {
        Address addr = new Address();
        addr.setId(id);
        addr.setMemberId(memberId);
        addr.setReceiverName("张三");
        addr.setReceiverPhone("13800138000");
        addr.setProvince("广东省");
        addr.setCity("深圳市");
        addr.setDistrict("南山区");
        addr.setDetail("科技园1号");
        return addr;
    }

    private CartItemVO cartItem(long skuId, int qty) {
        CartItemVO vo = new CartItemVO();
        vo.setSkuId(skuId);
        vo.setSpuId(1L);
        vo.setShopId(100L);
        vo.setPrice(new BigDecimal("5999.00"));
        vo.setQuantity(qty);
        vo.setSelected(true);
        vo.setAddTime(LocalDateTime.now());
        return vo;
    }

    @Test
    @DisplayName("confirm: 试算金额正确，生成 confirmToken")
    void confirmCalculatesAmountCorrectly() {
        Long userId = 1L;
        List<CartItemVO> cartItems = List.of(
                cartItem(10L, 2),
                cartItem(11L, 1));
        List<Sku> skus = List.of(
                sku(10L, new BigDecimal("5999.00")),
                sku(11L, new BigDecimal("99.00")));

        when(cartService.getSelectedItems(userId)).thenReturn(cartItems);
        when(skuMapper.selectBatchIds(any())).thenReturn(skus);
        when(spuMapper.selectById(anyLong())).thenReturn(spu(1L));
        when(addressMapper.selectById(1L)).thenReturn(address(1L, userId));
        when(stockService.getAvailableStock(anyLong())).thenReturn(100);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        OrderConfirmVO vo = orderService.confirm(userId, 1L, null, null, true, null);

        // 总金额 = 5999*2 + 99*1 = 12097
        assertThat(vo.getTotalAmount()).isEqualByComparingTo("12097.00");
        assertThat(vo.getPayAmount()).isEqualByComparingTo("12097.00");
        assertThat(vo.getTotalQuantity()).isEqualTo(3);
        assertThat(vo.getItems()).hasSize(2);
        assertThat(vo.getConfirmToken()).isNotBlank();
        assertThat(vo.getReceiver().getName()).isEqualTo("张三");

        // confirmToken 存入 Redis
        verify(valueOps).set(anyString(), anyString(), eq(15L), any());
    }

    @Test
    @DisplayName("confirm: 购物车为空抛 30005")
    void confirmThrowsWhenCartEmpty() {
        when(cartService.getSelectedItems(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.confirm(1L, 1L, null, null, true, null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.CART_IS_EMPTY.getCode());
    }

    @Test
    @DisplayName("confirm: 地址不属于当前用户抛 20001")
    void confirmThrowsWhenAddressNotOwned() {
        when(cartService.getSelectedItems(1L)).thenReturn(List.of(cartItem(10L, 1)));
        when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku(10L, new BigDecimal("99.00"))));
        when(spuMapper.selectById(anyLong())).thenReturn(spu(1L));
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 999L)); // 别人的地址

        assertThatThrownBy(() -> orderService.confirm(1L, 1L, null, null, true, null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("submit: 正常下单——锁库存 + 建订单 + 建订单项 + 清购物车")
    void submitCreatesOrderWithStockLock() {
        Long userId = 1L;
        List<CartItemVO> cartItems = List.of(cartItem(10L, 2));
        List<Sku> skus = List.of(sku(10L, new BigDecimal("5999.00")));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("token");
        when(cartService.getSelectedItems(userId)).thenReturn(cartItems);
        when(skuMapper.selectBatchIds(any())).thenReturn(skus);
        when(spuMapper.selectById(anyLong())).thenReturn(spu(1L));
        when(addressMapper.selectById(1L)).thenReturn(address(1L, userId));
        when(stockService.preoccupyRedis(anyLong(), eq(2))).thenReturn(true);
        when(stockService.preoccupyDB(anyLong(), eq(2))).thenReturn(true);

        OrderDetailVO vo = orderService.submit(userId, 1L, null, null, true, null, "token");

        // 验证订单创建
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Order order = orderCaptor.getValue();
        assertThat(order.getOrderNo()).isNotBlank();
        assertThat(order.getMemberId()).isEqualTo(userId);
        assertThat(order.getPayAmount()).isEqualByComparingTo("11998.00"); // 5999*2
        assertThat(order.getOrderStatus()).isEqualTo(OrderService.STATUS_PENDING_PAY);
        assertThat(order.getPayStatus()).isEqualTo(OrderService.PAY_UNPAID);

        // 验证订单项创建
        verify(orderItemMapper).insert(any(OrderItem.class));

        // 验证库存锁记录创建
        verify(stockService).createStockLock(eq(10L), any(), anyString(), eq(2));

        // 验证 Redis 库存预占和 DB 双保险
        verify(stockService).preoccupyRedis(10L, 2);
        verify(stockService).preoccupyDB(10L, 2);

        // 验证购物车清理
        verify(cartService).removeItems(eq(userId), any());

        // 验证 confirmToken 已消费
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("submit: confirmToken 已过期抛参数错误")
    void submitThrowsWhenTokenExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> orderService.submit(1L, 1L, null, null, true, null, "expired"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());
    }

    @Test
    @DisplayName("submit: Redis 库存不足抛 30002，不建订单")
    void submitThrowsWhenRedisStockNotEnough() {
        Long userId = 1L;
        List<CartItemVO> cartItems = List.of(cartItem(10L, 2));
        List<Sku> skus = List.of(sku(10L, new BigDecimal("5999.00")));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("token");
        when(cartService.getSelectedItems(userId)).thenReturn(cartItems);
        when(skuMapper.selectBatchIds(any())).thenReturn(skus);
        when(spuMapper.selectById(anyLong())).thenReturn(spu(1L));
        when(addressMapper.selectById(1L)).thenReturn(address(1L, userId));
        when(stockService.preoccupyRedis(10L, 2)).thenReturn(false);

        assertThatThrownBy(() -> orderService.submit(userId, 1L, null, null, true, null, "token"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.STOCK_NOT_ENOUGH.getCode());

        verify(orderMapper, never()).insert(any(Order.class));
        verify(stockService, never()).preoccupyDB(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("submit: DB 库存双保险失败——释放 Redis + 抛异常 + 不建订单")
    void submitThrowsWhenDBStockCheckFails() {
        Long userId = 1L;
        List<CartItemVO> cartItems = List.of(cartItem(10L, 2));
        List<Sku> skus = List.of(sku(10L, new BigDecimal("5999.00")));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("token");
        when(cartService.getSelectedItems(userId)).thenReturn(cartItems);
        when(skuMapper.selectBatchIds(any())).thenReturn(skus);
        when(spuMapper.selectById(anyLong())).thenReturn(spu(1L));
        when(addressMapper.selectById(1L)).thenReturn(address(1L, userId));
        when(stockService.preoccupyRedis(10L, 2)).thenReturn(true);
        when(stockService.preoccupyDB(10L, 2)).thenReturn(false);

        assertThatThrownBy(() -> orderService.submit(userId, 1L, null, null, true, null, "token"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.STOCK_NOT_ENOUGH.getCode());

        // Redis 库存已被释放
        verify(stockService).releaseRedis(10L, 2);
        // 没有建订单
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("detail: 越权查看他人订单抛 20001")
    void detailThrowsWhenNotOwner() {
        Order order = new Order();
        order.setId(1L);
        order.setMemberId(999L); // 别人的订单
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThatThrownBy(() -> orderService.detail(1L, 1L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("detail: 订单不存在抛 30003")
    void detailThrowsWhenNotFound() {
        when(orderMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> orderService.detail(1L, 404L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.ORDER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("金额计算精度：BigDecimal 无浮点误差")
    void amountCalculationHasNoFloatingPointError() {
        Long userId = 1L;
        // 19.99 * 3 = 59.97
        List<CartItemVO> cartItems = List.of(cartItem(10L, 3));
        List<Sku> skus = List.of(sku(10L, new BigDecimal("19.99")));

        when(cartService.getSelectedItems(userId)).thenReturn(cartItems);
        when(skuMapper.selectBatchIds(any())).thenReturn(skus);
        when(spuMapper.selectById(anyLong())).thenReturn(spu(1L));
        when(addressMapper.selectById(1L)).thenReturn(address(1L, userId));
        when(stockService.getAvailableStock(anyLong())).thenReturn(100);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        OrderConfirmVO vo = orderService.confirm(userId, 1L, null, null, true, null);

        assertThat(vo.getTotalAmount()).isEqualByComparingTo("59.97");
        assertThat(vo.getPayAmount()).isEqualByComparingTo("59.97");
        assertThat(vo.getItems().get(0).getSubtotal()).isEqualByComparingTo("59.97");
    }
}

