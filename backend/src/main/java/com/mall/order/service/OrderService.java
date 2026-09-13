package com.mall.order.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.mall.stock.entity.StockLock;
import com.mall.stock.service.StockService;
import com.mall.system.entity.Address;
import com.mall.system.mapper.AddressMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务。
 * <p>
 * confirm: 试算价格（不锁库存、不建订单），生成 confirmToken 存 Redis。
 * submit: @Transactional 内 Redis Lua 预占 + DB 双保险 + 建订单/订单项/库存锁。
 */
@Slf4j
@Service
public class OrderService {

    // 订单状态常量
    public static final int STATUS_PENDING_PAY = 0;
    public static final int STATUS_PENDING_SHIP = 1;
    public static final int STATUS_SHIPPED = 2;
    public static final int STATUS_RECEIVED = 3;
    public static final int STATUS_FINISHED = 4;
    public static final int STATUS_CANCELLED = 5;

    // 支付状态
    public static final int PAY_UNPAID = 0;
    public static final int PAY_PAID = 1;

    private static final String CONFIRM_TOKEN_PREFIX = "order:confirm:";
    private static final long CONFIRM_TOKEN_TTL_MINUTES = 15;
    private static final String ORDER_NO_SEQ_KEY = "order:seq";
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final AddressMapper addressMapper;
    private final StockService stockService;
    private final CartService cartService;
    private final StringRedisTemplate redisTemplate;

    public OrderService(OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper,
                        OrderStatusLogMapper orderStatusLogMapper,
                        SkuMapper skuMapper,
                        SpuMapper spuMapper,
                        AddressMapper addressMapper,
                        StockService stockService,
                        CartService cartService,
                        StringRedisTemplate redisTemplate) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.addressMapper = addressMapper;
        this.stockService = stockService;
        this.cartService = cartService;
        this.redisTemplate = redisTemplate;
    }

    // ==================== confirm 试算 ====================

    public OrderConfirmVO confirm(Long userId, Long addressId, Long skuId, Integer quantity,
                                   boolean fromCart, String remark) {
        // 收集商品项
        List<CartItemVO> cartItems;
        if (fromCart) {
            cartItems = cartService.getSelectedItems(userId);
            if (cartItems.isEmpty()) {
                throw new MallException(ErrorCode.Biz.CART_IS_EMPTY);
            }
        } else {
            if (skuId == null || quantity == null || quantity < 1) {
                throw new MallException(ErrorCode.Param.INVALID_PARAM, "直接购买需指定 SKU 和数量");
            }
            cartItems = List.of(buildDirectBuyItem(userId, skuId, quantity));
        }

        // 校验 SKU & SPU 可见性
        List<Sku> skus = validateSkus(cartItems);

        // 收货地址
        Address address = validateAddress(userId, addressId);

        // 构建试算项
        List<OrderConfirmVO.OrderItemVO> itemVOs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (int i = 0; i < cartItems.size(); i++) {
            CartItemVO cartItem = cartItems.get(i);
            Sku sku = skus.get(i);

            OrderConfirmVO.OrderItemVO itemVO = new OrderConfirmVO.OrderItemVO();
            itemVO.setSkuId(sku.getId());
            itemVO.setSpuId(sku.getSpuId());
            itemVO.setProductName(sku.getName());
            itemVO.setProductImage(sku.getImage());
            itemVO.setAttrs(sku.getAttrs());
            itemVO.setPrice(sku.getPrice());
            itemVO.setQuantity(cartItem.getQuantity());
            BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            itemVO.setSubtotal(subtotal);
            itemVO.setStock(stockService.getAvailableStock(sku.getId()));

            itemVOs.add(itemVO);
            totalAmount = totalAmount.add(subtotal);
            totalQuantity += cartItem.getQuantity();
        }

        // 运费 & 优惠（v1 简化：运费 0，优惠 0）
        BigDecimal freightAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payAmount = totalAmount.add(freightAmount).subtract(discountAmount);

        // 收货信息
        OrderConfirmVO.ReceiverVO receiver = new OrderConfirmVO.ReceiverVO();
        receiver.setName(address.getReceiverName());
        receiver.setPhone(address.getReceiverPhone());
        receiver.setProvince(address.getProvince());
        receiver.setCity(address.getCity());
        receiver.setDistrict(address.getDistrict());
        receiver.setAddress(address.getDetail());

        // 生成 confirmToken（防重复提交）
        String token = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(
                CONFIRM_TOKEN_PREFIX + userId + ":" + token,
                token,
                CONFIRM_TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES);

        OrderConfirmVO vo = new OrderConfirmVO();
        vo.setConfirmToken(token);
        vo.setItems(itemVOs);
        vo.setReceiver(receiver);
        vo.setTotalAmount(totalAmount);
        vo.setFreightAmount(freightAmount);
        vo.setDiscountAmount(discountAmount);
        vo.setPayAmount(payAmount);
        vo.setTotalQuantity(totalQuantity);
        return vo;
    }

    // ==================== submit 下单 ====================

    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO submit(Long userId, Long addressId, Long skuId, Integer quantity,
                                boolean fromCart, String remark, String confirmToken) {
        // 1. 校验 confirmToken（消费即删，防重复提交）
        String tokenKey = CONFIRM_TOKEN_PREFIX + userId + ":" + confirmToken;
        String stored = redisTemplate.opsForValue().get(tokenKey);
        if (stored == null) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "确认令牌已过期，请重新试算");
        }
        redisTemplate.delete(tokenKey);

        // 2. 收集商品项
        List<CartItemVO> cartItems;
        if (fromCart) {
            cartItems = cartService.getSelectedItems(userId);
            if (cartItems.isEmpty()) {
                throw new MallException(ErrorCode.Biz.CART_IS_EMPTY);
            }
        } else {
            if (skuId == null || quantity == null || quantity < 1) {
                throw new MallException(ErrorCode.Param.INVALID_PARAM, "直接购买需指定 SKU 和数量");
            }
            cartItems = List.of(buildDirectBuyItem(userId, skuId, quantity));
        }

        // 3. 校验 SKU & SPU
        List<Sku> skus = validateSkus(cartItems);

        // 4. 收货地址
        Address address = validateAddress(userId, addressId);

        // 5. 计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (int i = 0; i < cartItems.size(); i++) {
            Sku sku = skus.get(i);
            int qty = cartItems.get(i).getQuantity();
            totalAmount = totalAmount.add(sku.getPrice().multiply(BigDecimal.valueOf(qty)));
            totalQuantity += qty;
        }
        BigDecimal freightAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payAmount = totalAmount.add(freightAmount).subtract(discountAmount);

        // 6. 生成订单号
        String orderNo = generateOrderNo();

        // 7. Redis Lua 原子预占库存
        List<PreoccupiedItem> preoccupied = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItemVO cartItem = cartItems.get(i);
            Sku sku = skus.get(i);
            int qty = cartItem.getQuantity();

            boolean redisOk = stockService.preoccupyRedis(sku.getId(), qty);
            if (!redisOk) {
                // 回滚已预占的 Redis 库存
                releaseAllRedis(preoccupied);
                throw new MallException(ErrorCode.Biz.STOCK_NOT_ENOUGH, sku.getName());
            }
            preoccupied.add(new PreoccupiedItem(sku.getId(), qty));
        }

        // 8. DB 双保险：available_stock >= ? 条件更新
        for (int i = 0; i < cartItems.size(); i++) {
            Sku sku = skus.get(i);
            int qty = cartItems.get(i).getQuantity();

            boolean dbOk = stockService.preoccupyDB(sku.getId(), qty);
            if (!dbOk) {
                // DB 与 Redis 不一致：回滚 Redis + 抛异常（事务回滚 DB）
                releaseAllRedis(preoccupied);
                log.error("DB 库存双保险失败: skuId={}, qty={}", sku.getId(), qty);
                throw new MallException(ErrorCode.Biz.STOCK_NOT_ENOUGH, sku.getName());
            }
        }

        // 9. 建订单
        Spu firstSpu = spuMapper.selectById(skus.get(0).getSpuId());
        Long shopId = firstSpu != null ? firstSpu.getShopId() : 0L;

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setMemberId(userId);
        order.setShopId(shopId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setFreightAmount(freightAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayStatus(PAY_UNPAID);
        order.setOrderStatus(STATUS_PENDING_PAY);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverAddress(address.getDetail());
        order.setMemberRemark(remark);
        orderMapper.insert(order);

        // 10. 建订单项
        for (int i = 0; i < cartItems.size(); i++) {
            CartItemVO cartItem = cartItems.get(i);
            Sku sku = skus.get(i);
            Spu spu = spuMapper.selectById(sku.getSpuId());

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setSpuId(sku.getSpuId());
            item.setSkuId(sku.getId());
            item.setSkuCode(sku.getSkuCode());
            item.setProductName(spu != null ? spu.getName() : sku.getName());
            item.setProductImage(sku.getImage());
            item.setAttrs(sku.getAttrs());
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(sku.getPrice());
            item.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItemMapper.insert(item);
        }

        // 11. 建库存锁记录
        for (PreoccupiedItem p : preoccupied) {
            stockService.createStockLock(p.skuId(), order.getId(), orderNo, p.qty());
        }

        // 12. 状态日志
        logStatus(order, null, STATUS_PENDING_PAY, 1, userId, "下单");

        // 13. 清购物车
        if (fromCart) {
            List<Long> skuIds = cartItems.stream().map(CartItemVO::getSkuId).toList();
            cartService.removeItems(userId, skuIds);
        }

        log.info("下单成功: orderNo={}, userId={}, payAmount={}", orderNo, userId, payAmount);

        // 14. 返回订单详情
        return buildOrderDetailVO(order, cartItems, skus);
    }

    // ==================== 查询 ====================

    public OrderDetailVO detail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }
        // 资源归属校验：买家不能查看他人订单
        if (!order.getMemberId().equals(userId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        return buildOrderDetailVO(order, items);
    }

    public Order findById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    public Order findByOrderNo(String orderNo) {
        return orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));
    }

    public void updateOrder(Order order) {
        orderMapper.updateById(order);
    }

    public void logStatus(Order order, Integer fromStatus, int toStatus, int operatorType,
                          Long operatorId, String content) {
        OrderStatusLog logEntry = new OrderStatusLog();
        logEntry.setOrderId(order.getId());
        logEntry.setOrderNo(order.getOrderNo());
        logEntry.setFromStatus(fromStatus);
        logEntry.setToStatus(toStatus);
        logEntry.setOperatorType(operatorType);
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorName(operatorId == null ? "system" : String.valueOf(operatorId));
        logEntry.setContent(content);
        orderStatusLogMapper.insert(logEntry);
    }

    // ==================== 私有方法 ====================

    private record PreoccupiedItem(Long skuId, int qty) {}

    private void releaseAllRedis(List<PreoccupiedItem> items) {
        for (PreoccupiedItem p : items) {
            try {
                stockService.releaseRedis(p.skuId(), p.qty());
            } catch (Exception e) {
                log.error("释放 Redis 预占失败: skuId={}, qty={}", p.skuId(), p.qty(), e);
            }
        }
    }

    private List<Sku> validateSkus(List<CartItemVO> cartItems) {
        List<Long> skuIds = cartItems.stream().map(CartItemVO::getSkuId).toList();
        List<Sku> skus = skuMapper.selectBatchIds(skuIds);
        if (skus.size() != cartItems.size()) {
            throw new MallException(ErrorCode.Biz.SKU_NOT_FOUND);
        }
        for (Sku sku : skus) {
            if (sku.getStatus() == null || sku.getStatus() != 1) {
                throw new MallException(ErrorCode.Biz.PRODUCT_OFFLINE);
            }
            Spu spu = spuMapper.selectById(sku.getSpuId());
            if (spu == null || spu.getStatus() == null || spu.getStatus() != 1) {
                throw new MallException(ErrorCode.Biz.PRODUCT_OFFLINE);
            }
        }
        return skus;
    }

    private Address validateAddress(Long userId, Long addressId) {
        if (addressId == null) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "收货地址不能为空");
        }
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "收货地址不存在");
        }
        // 资源归属校验
        if (!address.getMemberId().equals(userId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        return address;
    }

    private CartItemVO buildDirectBuyItem(Long userId, Long skuId, int quantity) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new MallException(ErrorCode.Biz.SKU_NOT_FOUND);
        }
        Spu spu = spuMapper.selectById(sku.getSpuId());
        CartItemVO vo = new CartItemVO();
        vo.setSkuId(sku.getId());
        vo.setSpuId(sku.getSpuId());
        vo.setShopId(spu != null ? spu.getShopId() : null);
        vo.setSkuCode(sku.getSkuCode());
        vo.setSkuName(sku.getName());
        vo.setSpuName(spu != null ? spu.getName() : null);
        vo.setImage(sku.getImage());
        vo.setPrice(sku.getPrice());
        vo.setQuantity(quantity);
        vo.setSelected(true);
        vo.setAddTime(LocalDateTime.now());
        return vo;
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(ORDER_NO_FMT);
        Long seq = redisTemplate.opsForValue().increment(ORDER_NO_SEQ_KEY);
        String seqPart = String.format("%06d", seq != null ? seq % 1000000 : 0);
        int random = (int) (Math.random() * 100);
        return timePart + seqPart + String.format("%02d", random);
    }

    private OrderDetailVO buildOrderDetailVO(Order order, List<CartItemVO> cartItems, List<Sku> skus) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayStatus(order.getPayStatus());
        vo.setPayMethod(order.getPayMethod());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setMemberRemark(order.getMemberRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setPaymentTime(order.getPaymentTime());

        List<OrderConfirmVO.OrderItemVO> items = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItemVO cartItem = cartItems.get(i);
            Sku sku = skus.get(i);
            OrderConfirmVO.OrderItemVO item = new OrderConfirmVO.OrderItemVO();
            item.setSkuId(sku.getId());
            item.setSpuId(sku.getSpuId());
            item.setProductName(sku.getName());
            item.setProductImage(sku.getImage());
            item.setAttrs(sku.getAttrs());
            item.setPrice(sku.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            items.add(item);
        }
        vo.setItems(items);
        return vo;
    }

    private OrderDetailVO buildOrderDetailVO(Order order, List<OrderItem> orderItems) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayStatus(order.getPayStatus());
        vo.setPayMethod(order.getPayMethod());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(
                (order.getReceiverProvince() == null ? "" : order.getReceiverProvince()) +
                (order.getReceiverCity() == null ? "" : order.getReceiverCity()) +
                (order.getReceiverDistrict() == null ? "" : order.getReceiverDistrict()) +
                (order.getReceiverAddress() == null ? "" : order.getReceiverAddress()));
        vo.setMemberRemark(order.getMemberRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setPaymentTime(order.getPaymentTime());

        List<OrderConfirmVO.OrderItemVO> items = orderItems.stream().map(item -> {
            OrderConfirmVO.OrderItemVO itemVO = new OrderConfirmVO.OrderItemVO();
            itemVO.setSkuId(item.getSkuId());
            itemVO.setSpuId(item.getSpuId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImage(item.getProductImage());
            itemVO.setAttrs(item.getAttrs());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());
            return itemVO;
        }).toList();
        vo.setItems(items);
        return vo;
    }
}


