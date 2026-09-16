-- 秒杀优惠券：Redis 原子预扣 + 写入可靠队列（Redis Stream）
-- KEYS[1] = seckill:stock:{templateId}      剩余库存
-- KEYS[2] = seckill:user:{templateId}:{uid} 该用户已领数量
-- KEYS[3] = seckill:stream                 待落库队列
-- ARGV[1] = perUserLimit
-- ARGV[2] = 用户计数 key 过期秒数
-- ARGV[3] = userId
-- ARGV[4] = templateId
-- ARGV[5] = requestId
-- ARGV[6] = traceId (用于异步落库时关联原请求链路)
-- ARGV[7] = spanId  (原请求当前 span，用于在异步落库时挂成其子 span)
-- 返回: 1 成功 / -1 已抢完 / -2 超过限领 / -3 未预热

local stockKey = KEYS[1]
local userKey = KEYS[2]
local streamKey = KEYS[3]

local perUserLimit = tonumber(ARGV[1])
local userTtl = tonumber(ARGV[2])
local userId = ARGV[3]
local templateId = ARGV[4]
local requestId = ARGV[5]
local traceId = ARGV[6]
local spanId = ARGV[7]

local stock = redis.call('GET', stockKey)
if not stock then
    return -3
end
if tonumber(stock) <= 0 then
    return -1
end

local userCount = tonumber(redis.call('GET', userKey) or '0')
if userCount >= perUserLimit then
    return -2
end

-- 原子：扣库存 + 记用户领取数 + 入可靠队列
redis.call('DECR', stockKey)
redis.call('INCR', userKey)
redis.call('EXPIRE', userKey, userTtl)
redis.call('XADD', streamKey, '*', 'userId', userId, 'templateId', templateId, 'requestId', requestId, 'traceId', traceId, 'spanId', spanId)

return 1
