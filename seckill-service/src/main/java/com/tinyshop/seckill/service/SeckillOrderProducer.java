package com.tinyshop.seckill.service;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderProducer {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "seckill-order-topic";

    public void sendOrderMessage(Long userId, Long productId) {
        String payload = userId + ":" + productId;
        rocketMQTemplate.syncSend(TOPIC, MessageBuilder.withPayload(payload).build());
    }
}
