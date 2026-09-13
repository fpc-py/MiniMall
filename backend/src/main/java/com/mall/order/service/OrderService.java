package com.mall.order.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.cart.service.CartService;
import com.mall.cart.vo.CartItemVO;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.entity.OrderStatusLog;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.OrderStatusLogMapper;
import com.mall.order.vo.OrderConfirmVO;
import com.mall.order.vo.OrderDetailVO;
import com.mall.order.vo.OrderListVO;
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
 * 订单状态机：05已取消 10待支付 20待发货 30待收货 40已完成
 * 每次状态流转写入 oms_order_status_log。
 * <p>
 * confirm: 试算价格（不锁库存、不建订单），生成 confirmToken 存 Redis。
 * submit: @Transactional 内 Redis Lua 预占 + DB 双保险 + 建订单/订单项/库存锁。
 */
@Slf4j
@Service
public class OrderService {

    // ===== 订单状态机 =====
    /** 待支付 */
    public static final int STATUS_PENDING_PAY = 10;
    /** 待发货 */
    public static final int STATUS_PENDING_SHIP = 20;
    /** 待收货 */
    public static final int STATUS_PENDING_RECEIVE = 30;
    /** 已完成 */
    public static final int STATUS_FINISHED = 40;
    /** 已取消 */
    public static final int STATUS_CANCELLED = 5;

    // 支付状态
    public static final int PAY_UNPAID = 0;
    public static final int PAY_PAID = 1;

    // 操作者类型
    public static final int OP_SYSTEM = 0;
    public static final int OP_USER = 1;
    public static final int OP_SHOP = 2;
    public static final int OP_ADMIN = 3;

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

        List<Sku> skus = validateSkus(cartItems);
        Address address = validateAddress(userId, addressId);

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
            itemVO.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            itemVO.setStock(stockService.getAvailableStock(sku.getId()));
            itemVOs.add(itemVO);

            totalAmount = totalAmount.add(itemVO.getSubtotal());
            totalQuantity += cartItem.getQuantity();
        }

        BigDecimal freightAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payAmount = totalAmount.add(freightAmount).subtract(discountAmount);

        OrderConfirmVO vo = new OrderConfirmVO();
        String confirmToken = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(CONFIRM_TOKEN_PREFIX + confirmToken, String.valueOf(userId),
                CONFIRM_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        vo.setConfirmToken(confirmToken);
        vo.setItems(itemVOs);

        OrderConfirmVO.ReceiverVO receiverVO = new OrderConfirmVO.ReceiverVO();
        receiverVO.setName(address.getReceiverName());
        receiverVO.setPhone(address.getReceiverPhone());
        receiverVO.setProvince(address.getProvince());
        receiverVO.setCity(address.getCity());
        receiverVO.setDistrict(address.getDistrict());
        receiverVO.setAddress(address.getDetail());
        vo.setReceiver(receiverVO);

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
        // 校验 confirmToken
        String tokenKey = CONFIRM_TOKEN_PREFIX + confirmToken;
        String tokenUserId = redisTemplate.opsForValue().get(tokenKey);
        if (tokenUserId == null) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "确认令牌已过期或无效，请重新确认");
        }
        redisTemplate.delete(tokenKey);

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

        List<Sku> skus = validateSkus(cartItems);
        Address address = validateAddress(userId, addressId);

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

        String orderNo = generateOrderNo();

        // Redis Lua 原子预占库存
        List<PreoccupiedItem> preoccupied = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItemVO cartItem = cartItems.get(i);
            Sku sku = skus.get(i);
            int qty = cartItem.getQuantity();

            boolean redisOk = stockService.preoccupyRedis(sku.getId(), qty);
            if (!redisOk) {
                releaseAllRedis(preoccupied);
                throw new MallException(ErrorCode.Biz.STOCK_NOT_ENOUGH, sku.getName());
            }
            preoccupied.add(new PreoccupiedItem(sku.getId(), qty));
        }

        // DB 双保险
        for (int i = 0; i < cartItems.size(); i++) {
            Sku sku = skus.get(i);
            int qty = cartItems.get(i).getQuantity();

            boolean dbOk = stockService.preoccupyDB(sku.getId(), qty);
            if (!dbOk) {
                releaseAllRedis(preoccupied);
                log.error("DB 库存双保险失败: skuId={}, qty={}", sku.getId(), qty);
                throw new MallException(ErrorCode.Biz.STOCK_NOT_ENOUGH, sku.getName());
            }
        }

        // 建订单
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

        // 建订单项
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

        // 建库存锁记录
        for (PreoccupiedItem p : preoccupied) {
            stockService.createStockLock(p.skuId(), order.getId(), orderNo, p.qty());
        }

        // 状态日志
        logStatus(order, null, STATUS_PENDING_PAY, OP_USER, userId, "下单");

        // 清购物车
        if (fromCart) {
            List<Long> skuIds = cartItems.stream().map(CartItemVO::getSkuId).toList();
            cartService.removeItems(userId, skuIds);
        }

        log.info("下单成功: orderNo={}, userId={}, payAmount={}", orderNo, userId, payAmount);

        return buildOrderDetailVO(order, cartItems, skus);
    }

    // ==================== 买家端 - 我的订单列表 ====================

    public PageResult<OrderListVO> myOrders(Long userId, PageQuery pageQuery, Integer orderStatus) {
        Page<Order> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .eq(Order::getMemberId, userId)
                .orderByDesc(Order::getCreateTime);
        if (orderStatus != null) {
            wrapper.eq(Order::getOrderStatus, orderStatus);
        }
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        return PageResult.of(result, this::toOrderListVO);
    }

    // ==================== 买家端 - 取消订单 ====================

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId, String cancelReason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }
        if (!order.getMemberId().equals(userId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        // 只有待支付才能取消
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_PENDING_PAY) {
            throw new MallException(ErrorCode.Biz.ORDER_STATUS_INVALID, "当前订单状态不允许取消");
        }

        // 释放锁库存
        stockService.releaseByOrder(order.getId());

        // 更新订单状态
        int fromStatus = order.getOrderStatus();
        order.setOrderStatus(STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(cancelReason);
        orderMapper.updateById(order);

        // 状态日志
        logStatus(order, fromStatus, STATUS_CANCELLED, OP_USER, userId,
                cancelReason != null ? "用户取消：" + cancelReason : "用户取消");
    }

    // ==================== 买家端 - 确认收货 ====================

    @Transactional(rollbackFor = Exception.class)
    public void receive(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }
        if (!order.getMemberId().equals(userId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        // 只有待收货才能确认收货
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_PENDING_RECEIVE) {
            throw new MallException(ErrorCode.Biz.ORDER_STATUS_INVALID, "当前订单状态不允许确认收货");
        }

        int fromStatus = order.getOrderStatus();
        order.setOrderStatus(STATUS_FINISHED);
        order.setReceiveTime(LocalDateTime.now());
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);

        logStatus(order, fromStatus, STATUS_FINISHED, OP_USER, userId, "确认收货");
    }

    // ==================== 商家端 - 订单列表 ====================

    public PageResult<OrderListVO> shopOrders(Long shopId, PageQuery pageQuery, Integer orderStatus) {
        Page<Order> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .eq(Order::getShopId, shopId)
                .orderByDesc(Order::getCreateTime);
        if (orderStatus != null) {
            wrapper.eq(Order::getOrderStatus, orderStatus);
        }
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        return PageResult.of(result, this::toOrderListVO);
    }

    // ==================== 商家端 - 发货 ====================

    @Transactional(rollbackFor = Exception.class)
    public void ship(Long shopId, Long orderId, String deliveryCompany, String deliveryNo) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }
        if (!order.getShopId().equals(shopId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        // 只有待发货才能发货
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_PENDING_SHIP) {
            throw new MallException(ErrorCode.Biz.ORDER_STATUS_INVALID, "当前订单状态不允许发货");
        }

        int fromStatus = order.getOrderStatus();
        order.setOrderStatus(STATUS_PENDING_RECEIVE);
        order.setDeliveryCompany(deliveryCompany);
        order.setDeliveryNo(deliveryNo);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 更新出库任务状态为已出库
        try {
            stockService.updateOutboundTaskShipped(orderId);
        } catch (Exception e) {
            log.warn("更新出库任务状态失败，不影响发货: orderId={}", orderId, e);
        }

        logStatus(order, fromStatus, STATUS_PENDING_RECEIVE, OP_SHOP, null,
                "商家发货：" + deliveryCompany + " " + deliveryNo);
    }

    // ==================== 查询 ====================

    public OrderDetailVO detail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }
        if (!order.getMemberId().equals(userId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN);
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        return buildOrderDetailVO(order, items);
    }

    public OrderDetailVO shopDetail(Long shopId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }
        if (!order.getShopId().equals(shopId)) {
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

    private OrderListVO toOrderListVO(Order order) {
        OrderListVO vo = new OrderListVO();
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
        vo.setReceiverAddress(
                (order.getReceiverProvince() == null ? "" : order.getReceiverProvince()) +
                (order.getReceiverCity() == null ? "" : order.getReceiverCity()) +
                (order.getReceiverDistrict() == null ? "" : order.getReceiverDistrict()) +
                (order.getReceiverAddress() == null ? "" : order.getReceiverAddress()));
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

        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        List<OrderConfirmVO.OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderConfirmVO.OrderItemVO i = new OrderConfirmVO.OrderItemVO();
            i.setSkuId(item.getSkuId());
            i.setSpuId(item.getSpuId());
            i.setProductName(item.getProductName());
            i.setProductImage(item.getProductImage());
            i.setAttrs(item.getAttrs());
            i.setPrice(item.getPrice());
            i.setQuantity(item.getQuantity());
            i.setSubtotal(item.getSubtotal());
            return i;
        }).toList();
        vo.setItems(itemVOs);
        return vo;
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
        vo.setDeliveryCompany(order.getDeliveryCompany());
        vo.setDeliveryNo(order.getDeliveryNo());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setReceiveTime(order.getReceiveTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());

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
        vo.setDeliveryCompany(order.getDeliveryCompany());
        vo.setDeliveryNo(order.getDeliveryNo());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setReceiveTime(order.getReceiveTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());

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
