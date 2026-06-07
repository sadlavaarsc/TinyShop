package com.tinyshop.seckill.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.tinyshop.seckill.entity.SeckillActivity;
import com.tinyshop.seckill.mapper.SeckillMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillService {
    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SeckillOrderProducer orderProducer;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    @SentinelResource(value = "seckill", blockHandler = "handleBlock")
    public boolean doSeckill(Long userId, Long activityId) {
        // 1. Redis 预减库存（Lua 原子操作）
        String script = "if redis.call('get', KEYS[1]) > '0' then return redis.call('decr', KEYS[1]) else return -1 end";
        Long stock = redisTemplate.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
            java.util.Collections.singletonList(STOCK_KEY_PREFIX + activityId)
        );
        if (stock == null || stock < 0) {
            return false; // 库存不足
        }
        // 2. 发送异步下单消息
        SeckillActivity activity = seckillMapper.selectById(activityId);
        orderProducer.sendOrderMessage(userId, activity.getProductId());
        return true;
    }

    public String handleBlock(Long userId, Long activityId, com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        return "活动太火爆，请稍后再试";
    }
}
