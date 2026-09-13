-- stock_release.lua
-- KEYS[1] = stock:sku:{skuId}
-- ARGV[1] = quantity
-- 释放冻结库存：frozen -= qty, available += qty
local key = KEYS[1]
local qty = tonumber(ARGV[1])
redis.call('HINCRBY', key, 'frozen', -qty)
redis.call('HINCRBY', key, 'available', qty)
return 1
