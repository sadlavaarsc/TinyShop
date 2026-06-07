package com.tinyshop.pay.service;

import java.math.BigDecimal;

/**
 * 支付 Service 接口
 *
 * @author TinyShop Team
 */
public interface PayService {

    /**
     * 模拟支付
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @param amount  支付金额
     * @return 是否成功
     */
    boolean pay(Long orderId, Long userId, BigDecimal amount);

    /**
     * 模拟支付回调
     *
     * @param orderId 订单ID
     * @param tradeNo 第三方流水号
     * @return 是否成功
     */
    boolean callback(Long orderId, String tradeNo);
}
