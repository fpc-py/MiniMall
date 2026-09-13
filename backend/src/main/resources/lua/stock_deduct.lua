-- stock_deduct.lua
-- KEYS[1] = stock:sku:{skuId}
-- ARGV[1] = quantity
-- 支付成功后扣减冻结库存：frozen -= qty（可用库存不回退）
local key = KEYS[1]
local qty = tonumber(ARGV[1])
redis.call('HINCRBY', key, 'frozen', -qty)
return 1
