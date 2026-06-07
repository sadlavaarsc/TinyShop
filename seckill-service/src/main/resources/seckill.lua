-- 秒杀库存扣减 Lua 脚本（原子操作）
-- KEYS[1]: 秒杀库存 key (seckill:stock:{activityId})
-- KEYS[2]: 用户已购标记 key (seckill:user:{activityId}:{userId})
-- ARGV[1]: 购买数量

local stockKey = KEYS[1]
local userKey = KEYS[2]
local quantity = tonumber(ARGV[1])

-- 1. 检查用户是否已购买（防止重复购买）
local hasBought = redis.call('exists', userKey)
if hasBought == 1 then
    return -1  -- 已购买，不能重复秒杀
end

-- 2. 检查库存是否充足
local stock = redis.call('get', stockKey)
if stock == false then
    return -2  -- 活动不存在或库存未初始化
end

stock = tonumber(stock)
if stock < quantity then
    return -3  -- 库存不足
end

-- 3. 原子扣减库存
redis.call('decrby', stockKey, quantity)

-- 4. 标记用户已购买
redis.call('setex', userKey, 86400, quantity)

return 1  -- 秒杀成功
