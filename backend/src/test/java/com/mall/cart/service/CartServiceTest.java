package com.mall.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.cart.vo.CartItemVO;
import com.mall.cart.vo.CartSummaryVO;
import com.mall.cart.vo.CartVO;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SkuMapper skuMapper;

    @Mock
    private SpuMapper spuMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private CartService cartService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        cartService = new CartService(redisTemplate, skuMapper, spuMapper);
    }

    private Sku sku(long id, BigDecimal price, Integer stock, Integer frozen, Integer status) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setSpuId(1L);
        sku.setSkuCode("SKU-" + id);
        sku.setName("黑色 256G");
        sku.setPrice(price);
        sku.setStock(stock);
        sku.setFrozenStock(frozen);
        sku.setStatus(status);
        sku.setSort(1);
        return sku;
    }

    private Spu spu(long id, int status, int audit) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setName("iPhone 15");
        spu.setShopId(100L);
        spu.setStatus(status);
        spu.setAuditStatus(audit);
        spu.setMainImage("http://img/iphone.jpg");
        return spu;
    }

    private String cartItemJson(CartItemVO vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("加购：正常写入 Redis Hash")
    void addItemWritesToRedis() throws Exception {
        Long userId = 1L;
        Long skuId = 10L;
        Sku sku = sku(skuId, new BigDecimal("5999.00"), 100, 5, 1);
        Spu spu = spu(1L, 1, 1);

        when(skuMapper.selectById(skuId)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(anyString(), anyString())).thenReturn(null);

        cartService.addItem(userId, skuId, 2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOps).put(eq("cart:1"), eq("10"), valueCaptor.capture());

        CartItemVO saved = objectMapper.readValue(valueCaptor.getValue(), CartItemVO.class);
        assertThat(saved.getSkuId()).isEqualTo(skuId);
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getSelected()).isTrue();
        assertThat(saved.getPrice()).isEqualByComparingTo("5999.00");
        assertThat(saved.getShopId()).isEqualTo(100L);
        assertThat(saved.getAddTime()).isNotNull();
    }

    @Test
    @DisplayName("加购：已存在同 SKU 则累加数量")
    void addItemAccumulatesQuantity() throws Exception {
        Long userId = 1L;
        Long skuId = 10L;
        Sku sku = sku(skuId, new BigDecimal("5999.00"), 100, 5, 1);
        Spu spu = spu(1L, 1, 1);

        CartItemVO existing = new CartItemVO();
        existing.setSkuId(skuId);
        existing.setQuantity(3);
        existing.setSelected(true);
        existing.setPrice(new BigDecimal("5999.00"));
        existing.setAddTime(LocalDateTime.now());

        when(skuMapper.selectById(skuId)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(anyString(), anyString())).thenReturn(cartItemJson(existing));

        cartService.addItem(userId, skuId, 2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOps).put(eq("cart:1"), eq("10"), valueCaptor.capture());

        CartItemVO saved = objectMapper.readValue(valueCaptor.getValue(), CartItemVO.class);
        assertThat(saved.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("加购：SKU 不存在抛 30014")
    void addItemThrowsWhenSkuNotFound() {
        when(skuMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> cartService.addItem(1L, 999L, 1))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.SKU_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("加购：商品已下架抛 30001")
    void addItemThrowsWhenProductOffline() {
        Sku sku = sku(10L, new BigDecimal("5999.00"), 100, 5, 1);
        Spu spu = spu(1L, 0, 1);

        when(skuMapper.selectById(10L)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);

        assertThatThrownBy(() -> cartService.addItem(1L, 10L, 1))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.PRODUCT_OFFLINE.getCode());
    }

    @Test
    @DisplayName("修改购物车项：数量和选中状态")
    void updateItemChangesQuantityAndSelected() throws Exception {
        CartItemVO existing = new CartItemVO();
        existing.setSkuId(10L);
        existing.setQuantity(2);
        existing.setSelected(true);
        existing.setPrice(new BigDecimal("5999.00"));

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(anyString(), anyString())).thenReturn(cartItemJson(existing));

        cartService.updateItem(1L, 10L, 5, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOps).put(eq("cart:1"), eq("10"), valueCaptor.capture());

        CartItemVO saved = objectMapper.readValue(valueCaptor.getValue(), CartItemVO.class);
        assertThat(saved.getQuantity()).isEqualTo(5);
        assertThat(saved.getSelected()).isFalse();
    }

    @Test
    @DisplayName("修改购物车项：不存在抛 30006")
    void updateItemThrowsWhenNotFound() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(anyString(), anyString())).thenReturn(null);

        assertThatThrownBy(() -> cartService.updateItem(1L, 10L, 5, true))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.CART_ITEM_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("删除购物车项")
    void removeItem() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.delete(anyString(), anyString())).thenReturn(1L);

        cartService.removeItem(1L, 10L);

        verify(hashOps).delete("cart:1", "10");
    }

    @Test
    @DisplayName("删除不存在的购物车项抛 30006")
    void removeItemThrowsWhenNotFound() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.delete(anyString(), anyString())).thenReturn(0L);

        assertThatThrownBy(() -> cartService.removeItem(1L, 10L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.CART_ITEM_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("空购物车返回零值合计")
    void summaryOfEmptyCart() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(anyString())).thenReturn(new HashMap<>());

        CartSummaryVO summary = cartService.summary(1L);

        assertThat(summary.getTotalQuantity()).isZero();
        assertThat(summary.getSelectedQuantity()).isZero();
        assertThat(summary.getSelectedAmount()).isEqualByComparingTo("0.00");
        assertThat(summary.getSelectedCount()).isZero();
    }

    @Test
    @DisplayName("合计：正确计算选中商品件数和金额")
    void summaryCalculatesCorrectly() throws Exception {
        Map<Object, Object> cartData = new LinkedHashMap<>();
        cartData.put("10", cartItemJson(item(10L, new BigDecimal("5999.00"), 2, true)));
        cartData.put("11", cartItemJson(item(11L, new BigDecimal("99.00"), 3, false)));
        cartData.put("12", cartItemJson(item(12L, new BigDecimal("199.00"), 1, true)));

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(anyString())).thenReturn(cartData);

        CartSummaryVO summary = cartService.summary(1L);

        // 总件数 = 2+3+1 = 6
        assertThat(summary.getTotalQuantity()).isEqualTo(6);
        // 选中件数 = 2+1 = 3
        assertThat(summary.getSelectedQuantity()).isEqualTo(3);
        // 选中金额 = 5999*2 + 199*1 = 12197
        assertThat(summary.getSelectedAmount()).isEqualByComparingTo("12197.00");
        // 选中种类 = 2
        assertThat(summary.getSelectedCount()).isEqualTo(2);
    }

    private CartItemVO item(Long skuId, BigDecimal price, int qty, boolean selected) {
        CartItemVO vo = new CartItemVO();
        vo.setSkuId(skuId);
        vo.setSpuId(1L);
        vo.setShopId(100L);
        vo.setPrice(price);
        vo.setQuantity(qty);
        vo.setSelected(selected);
        vo.setAddTime(LocalDateTime.now());
        return vo;
    }

    @Test
    @DisplayName("购物车列表：按店铺分组，实时补填可售库存")
    void getCartGroupsByShop() {
        Map<Object, Object> cartData = new LinkedHashMap<>();
        cartData.put("10", cartItemJson(item(10L, new BigDecimal("5999.00"), 2, true)));
        cartData.put("11", cartItemJson(item(11L, new BigDecimal("99.00"), 1, true)));

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(anyString())).thenReturn(cartData);

        // 模拟 fillStock 查询
        Sku sku1 = sku(10L, new BigDecimal("5999.00"), 100, 5, 1);
        Sku sku2 = sku(11L, new BigDecimal("99.00"), 10, 0, 1);
        when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku1, sku2));

        CartVO cart = cartService.getCart(1L);

        assertThat(cart.getShops()).hasSize(1);
        assertThat(cart.getShops().get(0).getShopId()).isEqualTo(100L);
        assertThat(cart.getShops().get(0).getItems()).hasSize(2);

        // 可售库存 = 100-5 = 95
        CartItemVO item0 = cart.getShops().get(0).getItems().get(0);
        assertThat(item0.getStock()).isEqualTo(95);
        assertThat(item0.getInStock()).isTrue();

        // 总件数 = 3，选中件数 = 3
        assertThat(cart.getTotalQuantity()).isEqualTo(3);
        assertThat(cart.getSelectedQuantity()).isEqualTo(3);
        // 选中金额 = 5999*2 + 99*1 = 12097
        assertThat(cart.getSelectedAmount()).isEqualByComparingTo("12097.00");
    }

    @Test
    @DisplayName("全选：遍历更新每个项的 selected 字段")
    void selectAllUpdatesAllItems() {
        Map<Object, Object> cartData = new LinkedHashMap<>();
        cartData.put("10", cartItemJson(item(10L, new BigDecimal("5999.00"), 2, true)));
        cartData.put("11", cartItemJson(item(11L, new BigDecimal("99.00"), 1, false)));

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(anyString())).thenReturn(cartData);

        cartService.selectAll(1L, false);

        // 应写回两次 put
        verify(hashOps).put(eq("cart:1"), eq("10"), anyString());
        verify(hashOps).put(eq("cart:1"), eq("11"), anyString());
    }

    @Test
    @DisplayName("可售库存不会出现负数")
    void availableStockIsNeverNegative() {
        Sku sku1 = sku(1L, new BigDecimal("10.00"), 2, 5, 1);
        assertThat(CartService.availableStock(sku1)).isZero();

        Sku sku2 = sku(2L, new BigDecimal("10.00"), 10, 0, 1);
        assertThat(CartService.availableStock(sku2)).isEqualTo(10);

        Sku sku3 = sku(3L, new BigDecimal("10.00"), null, null, 1);
        assertThat(CartService.availableStock(sku3)).isZero();
    }

    @Test
    @DisplayName("下单后移除已购 SKU")
    void removeItemsAfterOrder() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        cartService.removeItems(1L, List.of(10L, 11L));

        verify(hashOps).delete(eq("cart:1"), eq("10"), eq("11"));
    }

    @Test
    @DisplayName("空列表不移除")
    void removeItemsEmptyDoesNothing() {
        cartService.removeItems(1L, List.of());
        verify(redisTemplate, never()).opsForHash();
    }

    @Test
    @DisplayName("获取选中项：只返回 selected=true 的项")
    void getSelectedItemsReturnsOnlySelected() {
        Map<Object, Object> cartData = new LinkedHashMap<>();
        cartData.put("10", cartItemJson(item(10L, new BigDecimal("5999.00"), 2, true)));
        cartData.put("11", cartItemJson(item(11L, new BigDecimal("99.00"), 1, false)));
        cartData.put("12", cartItemJson(item(12L, new BigDecimal("199.00"), 1, true)));

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(anyString())).thenReturn(cartData);

        List<CartItemVO> selected = cartService.getSelectedItems(1L);

        assertThat(selected).hasSize(2);
        assertThat(selected).allSatisfy(item -> assertThat(item.getSelected()).isTrue());
    }
}



