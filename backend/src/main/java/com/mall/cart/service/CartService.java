package com.mall.cart.service;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mall.cart.vo.CartItemVO;
import com.mall.cart.vo.CartVO;
import com.mall.cart.vo.CartSummaryVO;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车服务：以 Redis Hash cart:{userId} 为主存储。
 * <p>
 * Hash field = skuId(String)，value = CartItemVO(JSON)。
 * 加购时写入商品快照（名称/图片/价格），查购物车时实时补填可售库存。
 */
@Slf4j
@Service
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final int MAX_CART_ITEMS = 50;
    private static final int MAX_ITEM_QUANTITY = 99;

    private final StringRedisTemplate redisTemplate;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final ObjectMapper objectMapper;

    public CartService(StringRedisTemplate redisTemplate,
                       SkuMapper skuMapper,
                       SpuMapper spuMapper) {
        this.redisTemplate = redisTemplate;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    // ==================== 增 ====================

    public void addItem(Long userId, Long skuId, int quantity) {
        if (userId == null) {
            throw new MallException(ErrorCode.Auth.UNAUTHORIZED);
        }
        if (skuId == null) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "SKU ID 不能为空");
        }
        if (quantity < 1) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "加购数量至少为 1");
        }

        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new MallException(ErrorCode.Biz.SKU_NOT_FOUND);
        }
        Spu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null) {
            throw new MallException(ErrorCode.Biz.PRODUCT_NOT_FOUND);
        }
        if (spu.getStatus() == null || spu.getStatus() != 1) {
            throw new MallException(ErrorCode.Biz.PRODUCT_OFFLINE);
        }

        String key = cartKey(userId);
        String field = String.valueOf(skuId);

        // 已存在则累加
        CartItemVO existing = getCartItem(key, field);
        int newQuantity = (existing != null ? existing.getQuantity() : 0) + quantity;
        if (newQuantity > MAX_ITEM_QUANTITY) {
            throw new MallException(ErrorCode.Biz.CART_LIMIT_EXCEEDED,
                    "单个商品加购数量不能超过 " + MAX_ITEM_QUANTITY);
        }

        // 检查购物车种类数
        if (existing == null) {
            Long size = redisTemplate.opsForHash().size(key);
            if (size != null && size >= MAX_CART_ITEMS) {
                throw new MallException(ErrorCode.Biz.CART_LIMIT_EXCEEDED,
                        "购物车最多 " + MAX_CART_ITEMS + " 种商品");
            }
        }

        CartItemVO item = buildCartItemVO(sku, spu, newQuantity, existing);
        writeCartItem(key, field, item);
        log.info("加购: userId={}, skuId={}, quantity={}", userId, skuId, newQuantity);
    }

    private CartItemVO buildCartItemVO(Sku sku, Spu spu, int quantity, CartItemVO existing) {
        CartItemVO vo = new CartItemVO();
        vo.setSkuId(sku.getId());
        vo.setSpuId(sku.getSpuId());
        vo.setShopId(spu.getShopId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setSkuName(sku.getName());
        vo.setSpuName(spu.getName());
        vo.setImage(sku.getImage() != null ? sku.getImage() : spu.getMainImage());
        vo.setPrice(sku.getPrice());
        vo.setQuantity(quantity);
        vo.setSelected(existing != null ? existing.getSelected() : Boolean.TRUE);
        vo.setAddTime(existing != null ? existing.getAddTime() : LocalDateTime.now());
        return vo;
    }

    // ==================== 改 ====================

    public void updateItem(Long userId, Long skuId, Integer quantity, Boolean selected) {
        String key = cartKey(userId);
        String field = String.valueOf(skuId);
        CartItemVO item = getCartItem(key, field);
        if (item == null) {
            throw new MallException(ErrorCode.Biz.CART_ITEM_NOT_FOUND);
        }
        if (quantity != null) {
            if (quantity < 1 || quantity > MAX_ITEM_QUANTITY) {
                throw new MallException(ErrorCode.Biz.CART_LIMIT_EXCEEDED,
                        "购买数量需在 1~" + MAX_ITEM_QUANTITY + " 之间");
            }
            item.setQuantity(quantity);
        }
        if (selected != null) {
            item.setSelected(selected);
        }
        writeCartItem(key, field, item);
    }

    /** 全选/全不选 */
    public void selectAll(Long userId, boolean selected) {
        String key = cartKey(userId);
        Map<Object, Object> all = redisTemplate.opsForHash().entries(key);
        if (all == null || all.isEmpty()) {
            return;
        }
        for (Map.Entry<Object, Object> entry : all.entrySet()) {
            CartItemVO vo = deserialize(entry.getValue().toString());
            if (vo != null) {
                vo.setSelected(selected);
                writeCartItem(key, entry.getKey().toString(), vo);
            }
        }
    }

    // ==================== 删 ====================

    public void removeItem(Long userId, Long skuId) {
        String key = cartKey(userId);
        String field = String.valueOf(skuId);
        Long deleted = redisTemplate.opsForHash().delete(key, field);
        if (deleted == null || deleted == 0) {
            throw new MallException(ErrorCode.Biz.CART_ITEM_NOT_FOUND);
        }
        log.info("删除购物车项: userId={}, skuId={}", userId, skuId);
    }

    public void clear(Long userId) {
        String key = cartKey(userId);
        redisTemplate.delete(key);
        log.info("清空购物车: userId={}", userId);
    }

    // ==================== 查 ====================

    public CartVO getCart(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> all = redisTemplate.opsForHash().entries(key);
        if (all == null || all.isEmpty()) {
            CartVO vo = new CartVO();
            vo.setShops(Collections.emptyList());
            vo.setTotalQuantity(0);
            vo.setSelectedQuantity(0);
            vo.setSelectedAmount(BigDecimal.ZERO);
            return vo;
        }

        List<CartItemVO> items = new ArrayList<>(all.size());
        for (Map.Entry<Object, Object> entry : all.entrySet()) {
            CartItemVO vo = deserialize(entry.getValue().toString());
            if (vo != null) {
                items.add(vo);
            }
        }

        // 实时补填可售库存
        fillStock(items);

        // 按店铺分组
        Map<Long, List<CartItemVO>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        CartItemVO::getShopId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<CartVO.ShopGroup> shops = grouped.entrySet().stream().map(e -> {
            CartVO.ShopGroup g = new CartVO.ShopGroup();
            g.setShopId(e.getKey());
            g.setItems(e.getValue());
            return g;
        }).toList();

        CartVO vo = new CartVO();
        vo.setShops(shops);
        vo.setTotalQuantity(items.stream().mapToInt(CartItemVO::getQuantity).sum());

        List<CartItemVO> selected = items.stream()
                .filter(CartItemVO::getSelected)
                .toList();
        vo.setSelectedQuantity(selected.stream().mapToInt(CartItemVO::getQuantity).sum());
        vo.setSelectedAmount(calcSelectedAmount(selected));

        return vo;
    }

    public CartSummaryVO summary(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> all = redisTemplate.opsForHash().entries(key);
        if (all == null || all.isEmpty()) {
            CartSummaryVO vo = new CartSummaryVO();
            vo.setTotalQuantity(0);
            vo.setSelectedQuantity(0);
            vo.setSelectedAmount(BigDecimal.ZERO);
            vo.setSelectedCount(0);
            return vo;
        }

        List<CartItemVO> items = new ArrayList<>(all.size());
        for (Map.Entry<Object, Object> entry : all.entrySet()) {
            CartItemVO vo = deserialize(entry.getValue().toString());
            if (vo != null) {
                items.add(vo);
            }
        }

        int totalQty = items.stream().mapToInt(CartItemVO::getQuantity).sum();
        List<CartItemVO> selected = items.stream()
                .filter(CartItemVO::getSelected)
                .toList();
        int selectedQty = selected.stream().mapToInt(CartItemVO::getQuantity).sum();

        CartSummaryVO vo = new CartSummaryVO();
        vo.setTotalQuantity(totalQty);
        vo.setSelectedQuantity(selectedQty);
        vo.setSelectedAmount(calcSelectedAmount(selected));
        vo.setSelectedCount(selected.size());
        return vo;
    }

    /**
     * 获取选中的购物车项（供下单 confirm 使用）。
     */
    public List<CartItemVO> getSelectedItems(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> all = redisTemplate.opsForHash().entries(key);
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        List<CartItemVO> items = new ArrayList<>(all.size());
        for (Map.Entry<Object, Object> entry : all.entrySet()) {
            CartItemVO vo = deserialize(entry.getValue().toString());
            if (vo != null && Boolean.TRUE.equals(vo.getSelected())) {
                items.add(vo);
            }
        }
        return items;
    }

    /**
     * 下单成功后从购物车移除已购买的 SKU。
     */
    public void removeItems(Long userId, List<Long> skuIds) {
        if (CollUtil.isEmpty(skuIds)) {
            return;
        }
        String key = cartKey(userId);
        Object[] fields = skuIds.stream().map(String::valueOf).toArray();
        redisTemplate.opsForHash().delete(key, fields);
    }

    // ==================== 私有方法 ====================

    private BigDecimal calcSelectedAmount(List<CartItemVO> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void fillStock(List<CartItemVO> items) {
        if (items.isEmpty()) {
            return;
        }
        List<Long> skuIds = items.stream().map(CartItemVO::getSkuId).toList();
        List<Sku> skus = skuMapper.selectBatchIds(skuIds);
        Map<Long, Sku> skuMap = skus.stream()
                .collect(Collectors.toMap(Sku::getId, s -> s, (a, b) -> a));

        for (CartItemVO item : items) {
            Sku sku = skuMap.get(item.getSkuId());
            if (sku == null) {
                item.setStock(0);
                item.setInStock(false);
            } else {
                int available = availableStock(sku);
                item.setStock(available);
                item.setInStock(available > 0 && sku.getStatus() != null && sku.getStatus() == 1);
                // 实时价格：展示最新价格，不回写 Redis 快照
                if (sku.getPrice() != null) {
                    item.setPrice(sku.getPrice());
                }
            }
        }
    }

    static int availableStock(Sku sku) {
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        int frozen = sku.getFrozenStock() == null ? 0 : sku.getFrozenStock();
        return Math.max(stock - frozen, 0);
    }

    private CartItemVO getCartItem(String key, String field) {
        Object raw = redisTemplate.opsForHash().get(key, field);
        return raw == null ? null : deserialize(raw.toString());
    }

    private void writeCartItem(String key, String field, CartItemVO vo) {
        try {
            redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(vo));
        } catch (Exception e) {
            log.error("写入购物车缓存失败: key={}, field={}", key, field, e);
            throw new MallException(ErrorCode.Server.CACHE_ERROR);
        }
    }

    private CartItemVO deserialize(String json) {
        try {
            return objectMapper.readValue(json, CartItemVO.class);
        } catch (Exception e) {
            log.error("购物车项反序列化失败: {}", e.getMessage());
            return null;
        }
    }
}

