package com.tinyshop.seckill.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.result.R;
import com.tinyshop.common.utils.SnowflakeIdUtil;
import com.tinyshop.seckill.entity.SeckillActivity;
import com.tinyshop.seckill.mapper.SeckillActivityMapper;
import com.tinyshop.seckill.mq.SeckillOrderProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 秒杀 Service
 *
 * @author TinyShop Team
 */
@Slf4j
@Service
public class SeckillService {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SeckillOrderProducer seckillOrderProducer;

    /** 秒杀库存 Redis Key 前缀 */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /** 用户已购标记前缀 */
    private static final String USER_BOUGHT_PREFIX = "seckill:user:";

    /** Lua 脚本 */
    private DefaultRedisScript<Long> seckillScript;

    @PostConstruct
    public void init() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("seckill.lua")));
        seckillScript.setResultType(Long.class);
    }

    /**
     * 查询进行中的秒杀活动列表
     */
    public List<SeckillActivity> listActivities() {
        return seckillActivityMapper.selectActiveList(LocalDateTime.now());
    }

    /**
     * 秒杀下单
     * 1. Redis Lua 脚本预扣库存（原子操作）
     * 2. 发送 RocketMQ 异步下单消息
     * 3. MySQL 乐观锁最终扣减库存
     *
     * @param activityId 秒杀活动ID
     * @param userId     用户ID
     * @return 是否秒杀成功
     */
    @SentinelResource(value = "seckillOrder", blockHandler = "seckillBlockHandler")
    public boolean seckillOrder(Long activityId, Long userId) {
        // 1. 参数校验
        if (activityId == null || userId == null) {
            throw new BusinessException(R.Code.BAD_REQUEST, "参数错误");
        }

        // 2. Redis Lua 脚本原子扣减库存 + 防重复购买
        String stockKey = STOCK_KEY_PREFIX + activityId;
        String userKey = USER_BOUGHT_PREFIX + activityId + ":" + userId;

        Long result = redisTemplate.execute(
                seckillScript,
                Collections.singletonList(stockKey),
                userKey, "1"
        );

        if (result == null) {
            log.error("Lua 脚本执行返回 null");
            return false;
        }

        if (result == -1) {
            log.warn("用户重复秒杀: userId={}, activityId={}", userId, activityId);
            throw new BusinessException(R.Code.SECKILL_REPEAT, "您已参与过该秒杀活动");
        }
        if (result == -2) {
            log.warn("秒杀活动未初始化: activityId={}", activityId);
            throw new BusinessException(R.Code.SECKILL_NOT_START, "秒杀活动不存在");
        }
        if (result == -3) {
            log.warn("秒杀库存不足: activityId={}", activityId);
            throw new BusinessException(R.Code.SECKILL_STOCK_EMPTY, "秒杀商品已售罄");
        }

        // 3. 发送异步下单消息到 RocketMQ
        try {
            seckillOrderProducer.sendOrderMessage(activityId, userId);
        } catch (Exception e) {
            // 消息发送失败，回滚 Redis 库存（补偿机制）
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.delete(userKey);
            log.error("秒杀订单消息发送失败，已回滚库存: activityId={}, userId={}", activityId, userId, e);
            throw new BusinessException(R.Code.INTERNAL_ERROR, "系统繁忙，请稍后重试");
        }

        log.info("秒杀成功: activityId={}, userId={}", activityId, userId);
        return true;
    }

    /**
     * Sentinel 限流降级处理
     */
    public boolean seckillBlockHandler(Long activityId, Long userId, BlockException ex) {
        log.warn("秒杀接口被限流: activityId={}, userId={}", activityId, userId);
        throw new BusinessException(R.Code.SECKILL_RATE_LIMIT, "活动太火爆，请稍后再试");
    }

    /**
     * 初始化秒杀库存到 Redis（活动开始前调用）
     */
    public void initStock(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            throw new BusinessException(R.Code.SECKILL_NOT_START, "秒杀活动不存在或未开始");
        }
        String stockKey = STOCK_KEY_PREFIX + activityId;
        redisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getStock()));
        log.info("秒杀库存已初始化到 Redis: activityId={}, stock={}", activityId, activity.getStock());
    }

    /**
     * MySQL 乐观锁扣减库存（异步消费消息时调用）
     */
    public boolean deductStockWithOptimisticLock(Long activityId, Integer quantity) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null || activity.getStock() < quantity) {
            return false;
        }
        int affected = seckillActivityMapper.deductStock(activityId, quantity, activity.getVersion());
        return affected > 0;
    }
}
