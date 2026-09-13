-- stock_preoccupy.lua
-- KEYS[1] = stock:sku:{skuId}  (Redis Hash with fields: available, frozen)
-- ARGV[1] = quantity
-- 返回: 1=成功 0=库存不足
local key = KEYS[1]
local qty = tonumber(ARGV[1])
local available = tonumber(redis.call('HGET', key, 'available') or '0')
if available == nil then available = 0 end
if available >= qty then
    redis.call('HINCRBY', key, 'available', -qty)
    redis.call('HINCRBY', key, 'frozen', qty)
    return 1
else
    return 0
end
