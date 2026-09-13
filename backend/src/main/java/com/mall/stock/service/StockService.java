package com.mall.stock.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.stock.entity.Stock;
import com.mall.stock.entity.StockLock;
import com.mall.stock.mapper.OutboundTaskMapper;
import com.mall.stock.mapper.StockLockMapper;
import com.mall.stock.mapper.StockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存服务：
 * - Redis Hash stock:sku:{skuId} 维护实时可用/冻结库存（Lua 原子操作）
 * - DB wms_stock 用 available_stock >= ? 条件更新做双保险，杜绝超卖
 */
@Slf4j
@Service
public class StockService {

    private static final String STOCK_KEY_PREFIX = "stock:sku:";
    private static final String STOCK_LOCK_TTL_KEY_PREFIX = "stock:lock:ttl:";
    private static final int LOCK_EXPIRE_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;
    private final StockMapper stockMapper;
    private final StockLockMapper stockLockMapper;
    private final OutboundTaskMapper outboundTaskMapper;

    private final DefaultRedisScript<Long> preoccupyScript;
    private final DefaultRedisScript<Long> releaseScript;
    private final DefaultRedisScript<Long> deductScript;

    /**
     * 构造器注入 Lua 脚本。
     */
    public StockService(StringRedisTemplate redisTemplate,
                        StockMapper stockMapper,
                        StockLockMapper stockLockMapper,
                        OutboundTaskMapper outboundTaskMapper,
                        DefaultRedisScript<Long> preoccupyScript,
                        DefaultRedisScript<Long> releaseScript,
                        DefaultRedisScript<Long> deductScript) {
        this.redisTemplate = redisTemplate;
        this.stockMapper = stockMapper;
        this.stockLockMapper = stockLockMapper;
        this.outboundTaskMapper = outboundTaskMapper;
        this.preoccupyScript = preoccupyScript;
        this.releaseScript = releaseScript;
        this.deductScript = deductScript;
    }

    private String stockKey(Long skuId) {
        return STOCK_KEY_PREFIX + skuId;
    }

    /**
     * 初始化 SKU 的 Redis 库存缓存（从 DB 加载）。
     * 若缓存已存在则跳过，避免覆盖运行期数据。
     */
    public void initStockCache(Long skuId) {
        String key = stockKey(skuId);
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }
        Stock stock = stockMapper.selectOne(Wrappers.<Stock>lambdaQuery()
                .eq(Stock::getSkuId, skuId)
                .eq(Stock::getWarehouseCode, "default"));
        if (stock == null) {
            log.warn("SKU {} 无库存记录，初始化为 0", skuId);
            redisTemplate.opsForHash().put(key, "available", "0");
            redisTemplate.opsForHash().put(key, "frozen", "0");
            return;
        }
        redisTemplate.opsForHash().put(key, "available", String.valueOf(stock.getAvailableStock()));
        redisTemplate.opsForHash().put(key, "frozen", String.valueOf(stock.getFrozenStock()));
    }

    /**
     * Redis 原子预占库存（Lua 脚本）。
     * @return true=成功 false=库存不足
     */
    public boolean preoccupyRedis(Long skuId, int quantity) {
        initStockCache(skuId);
        Long result = redisTemplate.execute(
                preoccupyScript,
                List.of(stockKey(skuId)),
                String.valueOf(quantity));
        return result != null && result == 1L;
    }

    /**
     * DB 双保险：用 available_stock >= ? 条件更新。
     * 成功后 frozen_stock += quantity, available_stock -= quantity。
     * @return true=成功 false=库存不足（此时需要回滚 Redis 预占）
     */
    public boolean preoccupyDB(Long skuId, int quantity) {
        LambdaUpdateWrapper<Stock> wrapper = Wrappers.<Stock>lambdaUpdate()
                .eq(Stock::getSkuId, skuId)
                .eq(Stock::getWarehouseCode, "default")
                .ge(Stock::getAvailableStock, quantity)
                .setSql("available_stock = available_stock - " + quantity)
                .setSql("frozen_stock = frozen_stock + " + quantity);
        int rows = stockMapper.update(null, wrapper);
        return rows > 0;
    }

    /**
     * 释放 Redis 预占库存（下单失败 / 超时关单）。
     */
    public void releaseRedis(Long skuId, int quantity) {
        redisTemplate.execute(
                releaseScript,
                List.of(stockKey(skuId)),
                String.valueOf(quantity));
    }

    /**
     * 释放 DB 锁定库存（超时关单时调用）。
     */
    public void releaseDB(Long skuId, int quantity) {
        LambdaUpdateWrapper<Stock> wrapper = Wrappers.<Stock>lambdaUpdate()
                .eq(Stock::getSkuId, skuId)
                .eq(Stock::getWarehouseCode, "default")
                .setSql("frozen_stock = frozen_stock - " + quantity)
                .setSql("available_stock = available_stock + " + quantity);
        stockMapper.update(null, wrapper);
    }

    /**
     * 支付成功后扣减冻结库存（Redis + DB）。
     * frozen -= quantity（可用库存不回退，因为已从可用转为冻结）。
     */
    public void deductFrozen(Long skuId, int quantity) {
        // Redis: frozen -= qty
        redisTemplate.execute(
                deductScript,
                List.of(stockKey(skuId)),
                String.valueOf(quantity));
        // DB: frozen_stock -= qty, total_stock -= qty
        LambdaUpdateWrapper<Stock> wrapper = Wrappers.<Stock>lambdaUpdate()
                .eq(Stock::getSkuId, skuId)
                .eq(Stock::getWarehouseCode, "default")
                .setSql("frozen_stock = frozen_stock - " + quantity)
                .setSql("total_stock = total_stock - " + quantity);
        stockMapper.update(null, wrapper);
    }

    /**
     * 创建库存锁定记录。
     */
    public StockLock createStockLock(Long skuId, Long orderId, String orderNo, int quantity) {
        StockLock lock = new StockLock();
        lock.setSkuId(skuId);
        lock.setOrderId(orderId);
        lock.setOrderNo(orderNo);
        lock.setQuantity(quantity);
        lock.setStatus(0); // 锁定中
        lock.setExpireTime(LocalDateTime.now().plusMinutes(LOCK_EXPIRE_MINUTES));
        stockLockMapper.insert(lock);
        return lock;
    }

    /**
     * 按订单释放所有锁定库存（超时关单）。
     */
    public void releaseByOrder(Long orderId) {
        List<StockLock> locks = stockLockMapper.selectList(Wrappers.<StockLock>lambdaQuery()
                .eq(StockLock::getOrderId, orderId)
                .eq(StockLock::getStatus, 0));
        for (StockLock lock : locks) {
            releaseRedis(lock.getSkuId(), lock.getQuantity());
            releaseDB(lock.getSkuId(), lock.getQuantity());
            lock.setStatus(2); // 已释放
            stockLockMapper.updateById(lock);
        }
    }

    /**
     * 查询 SKU 可售库存（优先 Redis，降级 DB）。
     */
    public int getAvailableStock(Long skuId) {
        String key = stockKey(skuId);
        Object available = redisTemplate.opsForHash().get(key, "available");
        if (available != null) {
            try {
                return Integer.parseInt(available.toString());
            } catch (NumberFormatException e) {
                log.warn("Redis 库存值非数字: skuId={}, value={}", skuId, available);
            }
        }
        Stock stock = stockMapper.selectOne(Wrappers.<Stock>lambdaQuery()
                .eq(Stock::getSkuId, skuId)
                .eq(Stock::getWarehouseCode, "default"));
        return stock == null ? 0 : (stock.getAvailableStock() == null ? 0 : stock.getAvailableStock());
    }

    /**
     * 出库任务标记为已出库（商家发货时调用）。
     */
    public void updateOutboundTaskShipped(Long orderId) {
        com.mall.stock.entity.OutboundTask task = outboundTaskMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<com.mall.stock.entity.OutboundTask>lambdaQuery()
                        .eq(com.mall.stock.entity.OutboundTask::getOrderId, orderId));
        if (task != null) {
            task.setStatus(2); // 已出库
            task.setOutboundTime(java.time.LocalDateTime.now());
            outboundTaskMapper.updateById(task);
        }
    }

}

