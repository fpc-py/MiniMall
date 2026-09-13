package com.mall.stock.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.stock.entity.Stock;
import com.mall.stock.entity.StockLock;
import com.mall.stock.mapper.OutboundTaskMapper;
import com.mall.stock.mapper.StockLockMapper;
import com.mall.stock.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private StockMapper stockMapper;
    @Mock private StockLockMapper stockLockMapper;
    @Mock private OutboundTaskMapper outboundTaskMapper;
    @Mock private HashOperations<String, Object, Object> hashOps;

    private DefaultRedisScript<Long> preoccupyScript;
    private DefaultRedisScript<Long> releaseScript;
    private DefaultRedisScript<Long> deductScript;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        preoccupyScript = new DefaultRedisScript<>("return 1", Long.class);
        releaseScript = new DefaultRedisScript<>("return 1", Long.class);
        deductScript = new DefaultRedisScript<>("return 1", Long.class);
        stockService = new StockService(redisTemplate, stockMapper, stockLockMapper,
                outboundTaskMapper, preoccupyScript, releaseScript, deductScript);
    }

    private Stock stock(long skuId, int available, int frozen) {
        Stock s = new Stock();
        s.setId(1L); s.setSkuId(skuId); s.setWarehouseCode("default");
        s.setAvailableStock(available); s.setFrozenStock(frozen);
        s.setTotalStock(available + frozen); return s;
    }

    @Test @DisplayName("initStockCache: loads from DB when cache missing")
    void initStockCacheLoadsFromDb() {
        when(redisTemplate.hasKey("stock:sku:100")).thenReturn(false);
        when(stockMapper.selectOne(any())).thenReturn(stock(100L, 50, 0));
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        stockService.initStockCache(100L);
        verify(hashOps).put("stock:sku:100", "available", "50");
        verify(hashOps).put("stock:sku:100", "frozen", "0");
    }

    @Test @DisplayName("initStockCache: skips when cache exists")
    void initStockCacheSkipsWhenExists() {
        when(redisTemplate.hasKey("stock:sku:100")).thenReturn(true);
        stockService.initStockCache(100L);
        verify(stockMapper, never()).selectOne(any());
    }

    @Test @DisplayName("preoccupyRedis: returns true on success")
    void preoccupyRedisReturnsTrue() {
        when(redisTemplate.hasKey("stock:sku:100")).thenReturn(false);
        when(stockMapper.selectOne(any())).thenReturn(stock(100L, 10, 0));
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(1L);
        assertThat(stockService.preoccupyRedis(100L, 3)).isTrue();
        verify(redisTemplate).execute(preoccupyScript, List.of("stock:sku:100"), "3");
    }

    @Test @DisplayName("preoccupyRedis: returns false when stock insufficient")
    void preoccupyRedisReturnsFalse() {
        when(redisTemplate.hasKey("stock:sku:100")).thenReturn(false);
        when(stockMapper.selectOne(any())).thenReturn(stock(100L, 1, 0));
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(0L);
        assertThat(stockService.preoccupyRedis(100L, 5)).isFalse();
    }

    @Test @DisplayName("preoccupyDB: returns true on success")
    void preoccupyDBSuccess() {
        when(stockMapper.update(any(), any())).thenReturn(1);
        assertThat(stockService.preoccupyDB(100L, 3)).isTrue();
        ArgumentCaptor<LambdaUpdateWrapper<Stock>> captor = ArgumentCaptor.captor();
        verify(stockMapper).update(eq(null), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("available_stock = available_stock - 3");
        assertThat(captor.getValue().getSqlSet()).contains("frozen_stock = frozen_stock + 3");
    }

    @Test @DisplayName("preoccupyDB: returns false when stock insufficient")
    void preoccupyDBFails() {
        when(stockMapper.update(any(), any())).thenReturn(0);
        assertThat(stockService.preoccupyDB(100L, 999)).isFalse();
    }

    @Test @DisplayName("releaseRedis: executes Lua script")
    void releaseRedisExecutes() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(1L);
        stockService.releaseRedis(100L, 2);
        verify(redisTemplate).execute(releaseScript, List.of("stock:sku:100"), "2");
    }

    @Test @DisplayName("releaseDB: updates stock correctly")
    void releaseDBSuccess() {
        when(stockMapper.update(any(), any())).thenReturn(1);
        stockService.releaseDB(100L, 2);
        ArgumentCaptor<LambdaUpdateWrapper<Stock>> captor = ArgumentCaptor.captor();
        verify(stockMapper).update(eq(null), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("frozen_stock = frozen_stock - 2");
        assertThat(captor.getValue().getSqlSet()).contains("available_stock = available_stock + 2");
    }

    @Test @DisplayName("deductFrozen: Redis + DB deduct")
    void deductFrozenExecutes() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(1L);
        when(stockMapper.update(any(), any())).thenReturn(1);
        stockService.deductFrozen(100L, 2);
        verify(redisTemplate).execute(deductScript, List.of("stock:sku:100"), "2");
        ArgumentCaptor<LambdaUpdateWrapper<Stock>> captor = ArgumentCaptor.captor();
        verify(stockMapper).update(eq(null), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("frozen_stock = frozen_stock - 2");
        assertThat(captor.getValue().getSqlSet()).contains("total_stock = total_stock - 2");
    }

    @Test @DisplayName("createStockLock: inserts lock record")
    void createStockLockInserts() {
        stockService.createStockLock(100L, 1L, "NO001", 2);
        ArgumentCaptor<StockLock> captor = ArgumentCaptor.forClass(StockLock.class);
        verify(stockLockMapper).insert(captor.capture());
        StockLock lock = captor.getValue();
        assertThat(lock.getSkuId()).isEqualTo(100L);
        assertThat(lock.getOrderId()).isEqualTo(1L);
        assertThat(lock.getQuantity()).isEqualTo(2);
        assertThat(lock.getStatus()).isEqualTo(0);
        assertThat(lock.getExpireTime()).isAfter(LocalDateTime.now());
    }

    @Test @DisplayName("releaseByOrder: releases all locks for order")
    void releaseByOrderReleasesAllLocks() {
        StockLock l1 = new StockLock(); l1.setId(1L); l1.setSkuId(100L); l1.setQuantity(2); l1.setStatus(0);
        StockLock l2 = new StockLock(); l2.setId(2L); l2.setSkuId(101L); l2.setQuantity(1); l2.setStatus(0);
        when(stockLockMapper.selectList(any())).thenReturn(List.of(l1, l2));
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(1L);
        when(stockMapper.update(any(), any())).thenReturn(1);
        stockService.releaseByOrder(1L);
        verify(stockLockMapper, times(2)).updateById(any(StockLock.class));
        verify(redisTemplate, times(2)).execute(any(DefaultRedisScript.class), anyList(), anyString());
        verify(stockMapper, times(2)).update(any(), any());
    }

    @Test @DisplayName("getAvailableStock: prefers Redis")
    void getAvailableStockPrefersRedis() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get("stock:sku:100", "available")).thenReturn("25");
        assertThat(stockService.getAvailableStock(100L)).isEqualTo(25);
        verify(stockMapper, never()).selectOne(any());
    }

    @Test @DisplayName("getAvailableStock: falls back to DB")
    void getAvailableStockFallsBackToDb() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get("stock:sku:100", "available")).thenReturn(null);
        when(stockMapper.selectOne(any())).thenReturn(stock(100L, 30, 0));
        assertThat(stockService.getAvailableStock(100L)).isEqualTo(30);
    }

    @Test @DisplayName("getAvailableStock: returns 0 when no data")
    void getAvailableStockReturnsZero() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get("stock:sku:200", "available")).thenReturn(null);
        when(stockMapper.selectOne(any())).thenReturn(null);
        assertThat(stockService.getAvailableStock(200L)).isEqualTo(0);
    }

    @Test @DisplayName("updateOutboundTaskShipped: marks as shipped")
    void updateOutboundTaskShipped() {
        com.mall.stock.entity.OutboundTask task = new com.mall.stock.entity.OutboundTask();
        task.setId(1L); task.setStatus(0);
        when(outboundTaskMapper.selectOne(any())).thenReturn(task);
        stockService.updateOutboundTaskShipped(1L);
        ArgumentCaptor<com.mall.stock.entity.OutboundTask> captor = ArgumentCaptor.captor();
        verify(outboundTaskMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(2);
        assertThat(captor.getValue().getOutboundTime()).isNotNull();
    }
}
