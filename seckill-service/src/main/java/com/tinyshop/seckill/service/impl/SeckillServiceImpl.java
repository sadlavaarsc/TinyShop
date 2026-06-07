package com.tinyshop.seckill.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.result.R;
import com.tinyshop.common.utils.SnowflakeIdUtil;
import com.tinyshop.seckill.entity.SeckillActivity;
import com.tinyshop.seckill.mapper.SeckillActivityMapper;
import com.tinyshop.seckill.mq.SeckillOrderProducer;
import com.tinyshop.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 秒杀 Service 实现类
 *
 * @author TinyShop Team
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SeckillOrderProducer seckillOrderProducer;

    /** 秒杀库存 Redis Key 前缀 */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /** 用户已购 Redis Key 前缀 */
    private static final String USER_KEY_PREFIX = "seckill:user:";

    private DefaultRedisScript<Long> seckillScript;

    @PostConstruct
    public void init() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("seckill.lua")));
        seckillScript.setResultType(Long.class);
    }

    @Override
    public List<SeckillActivity> listActivities() {
        return seckillActivityMapper.selectActiveList(LocalDateTime.now());
    }

    @Override
    @SentinelResource(value = "seckillOrder", blockHandler = "seckillBlockHandler")
    public boolean seckillOrder(Long activityId, Long userId) {
        // 1. 校验活动状态
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(R.Code.SECKILL_NOT_START, "秒杀活动不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(R.Code.SECKILL_NOT_START, "秒杀活动未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(R.Code.SECKILL_ALREADY_END, "秒杀活动已结束");
        }

        // 2. Redis Lua 脚本原子扣减库存
        String stockKey = STOCK_KEY_PREFIX + activityId;
        String userKey = USER_KEY_PREFIX + activityId + ":" + userId;

        Long result = stringRedisTemplate.execute(
                seckillScript,
                Arrays.asList(stockKey, userKey),
                "1"
        );

        if (result == null) {
            log.error("Lua 脚本执行返回 null");
            return false;
        }

        if (result == -1) {
            log.warn("用户重复秒杀: activityId={}, userId={}", activityId, userId);
            throw new BusinessException(R.Code.SECKILL_REPEAT, "您已经参与过该秒杀活动");
        }
        if (result == -2) {
            log.warn("秒杀活动库存未初始化: activityId={}", activityId);
            return false;
        }
        if (result == -3) {
            log.warn("秒杀库存不足: activityId={}", activityId);
            return false;
        }

        // 3. 发送异步下单消息到 RocketMQ
        try {
            seckillOrderProducer.sendOrderMessage(activityId, userId);
        } catch (Exception e) {
            log.error("发送秒杀订单消息失败，回滚 Redis 库存: activityId={}, userId={}", activityId, userId);
            // 回滚 Redis 库存
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(userKey);
            return false;
        }

        log.info("秒杀成功: activityId={}, userId={}", activityId, userId);
        return true;
    }

    /**
     * Sentinel 限流降级方法
     */
    public boolean seckillBlockHandler(Long activityId, Long userId, BlockException ex) {
        log.warn("秒杀接口被限流: activityId={}, userId={}", activityId, userId);
        throw new BusinessException(R.Code.SECKILL_RATE_LIMIT, "活动太火爆，请稍后再试");
    }

    @Override
    public void handleSeckillOrder(Long activityId, Long userId) {
        // MQ 消费端调用此方法进行异步下单
        // 1. 查询活动信息
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            log.error("秒杀活动不存在: activityId={}", activityId);
            return;
        }

        // 2. MySQL 乐观锁兜底扣减库存
        int affected = seckillActivityMapper.deductStock(activityId, 1, activity.getVersion());
        if (affected <= 0) {
            log.warn("MySQL 乐观锁扣减库存失败: activityId={}", activityId);
            return;
        }

        // 3. 创建订单（实际应调用订单服务）
        log.info("异步创建秒杀订单: activityId={}, userId={}, productId={}",
                activityId, userId, activity.getProductId());
    }

    /**
     * 初始化秒杀库存到 Redis（活动开始前调用）
     */
    public void initStock(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity != null) {
            String stockKey = STOCK_KEY_PREFIX + activityId;
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getStock()));
            log.info("初始化秒杀库存到 Redis: activityId={}, stock={}", activityId, activity.getStock());
        }
    }
}
