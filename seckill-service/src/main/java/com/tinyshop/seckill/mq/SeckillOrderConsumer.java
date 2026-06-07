package com.tinyshop.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.tinyshop.common.utils.SnowflakeIdUtil;
import com.tinyshop.seckill.entity.SeckillActivity;
import com.tinyshop.seckill.mapper.SeckillActivityMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 秒杀订单 MQ 消费者
 * 异步处理秒杀下单，MySQL 乐观锁最终扣减库存
 *
 * @author TinyShop Team
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "seckill-order-topic",
        consumerGroup = "seckill-order-consumer-group"
)
public class SeckillOrderConsumer implements RocketMQListener<String> {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Override
    public void onMessage(String message) {
        log.info("收到秒杀订单消息: {}", message);

        try {
            Map<String, Object> msg = JSON.parseObject(message, Map.class);
            Long activityId = Long.valueOf(msg.get("activityId").toString());
            Long userId = Long.valueOf(msg.get("userId").toString());

            // 1. 查询秒杀活动
            SeckillActivity activity = seckillActivityMapper.selectById(activityId);
            if (activity == null || activity.getStatus() != 1) {
                log.error("秒杀活动不存在或已结束: activityId={}", activityId);
                return;
            }

            // 2. 乐观锁扣减库存
            int retry = 3;
            boolean success = false;
            while (retry-- > 0 && !success) {
                activity = seckillActivityMapper.selectById(activityId);
                if (activity.getStock() <= 0) {
                    log.warn("秒杀库存不足: activityId={}", activityId);
                    return;
                }
                int affected = seckillActivityMapper.deductStock(activityId, 1, activity.getVersion());
                success = affected > 0;
            }

            if (!success) {
                log.error("秒杀库存扣减失败（乐观锁冲突）: activityId={}, userId={}", activityId, userId);
                // TODO: 补偿 Redis 库存，通知用户秒杀失败
                return;
            }

            // 3. 生成订单（可调用 order-service 或本地写入）
            Long orderId = SnowflakeIdUtil.nextId();
            log.info("秒杀订单创建成功: orderId={}, activityId={}, userId={}, productId={}",
                    orderId, activityId, userId, activity.getProductId());

            // TODO: 写入订单表，发送支付超时延迟消息

        } catch (Exception e) {
            log.error("秒杀订单消息处理失败: message={}", message, e);
            // 消费异常，RocketMQ 会重试
            throw new RuntimeException("消息处理失败", e);
        }
    }
}
