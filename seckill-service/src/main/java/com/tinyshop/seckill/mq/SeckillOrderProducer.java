package com.tinyshop.seckill.mq;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀订单 MQ 生产者
 *
 * @author TinyShop Team
 */
@Slf4j
@Component
public class SeckillOrderProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /** 秒杀订单 Topic */
    private static final String SECKILL_ORDER_TOPIC = "seckill-order-topic";

    /**
     * 发送秒杀订单消息
     *
     * @param activityId 秒杀活动ID
     * @param userId     用户ID
     */
    public void sendOrderMessage(Long activityId, Long userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("activityId", activityId);
        message.put("userId", userId);
        message.put("timestamp", System.currentTimeMillis());

        try {
            rocketMQTemplate.syncSend(
                    SECKILL_ORDER_TOPIC,
                    MessageBuilder.withPayload(JSON.toJSONString(message)).build()
            );
            log.info("秒杀订单消息发送成功: activityId={}, userId={}", activityId, userId);
        } catch (Exception e) {
            log.error("秒杀订单消息发送失败: activityId={}, userId={}", activityId, userId, e);
            throw new RuntimeException("消息发送失败", e);
        }
    }
}
