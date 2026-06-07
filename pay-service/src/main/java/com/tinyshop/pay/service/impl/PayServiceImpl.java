package com.tinyshop.pay.service.impl;

import com.tinyshop.common.utils.SnowflakeIdUtil;
import com.tinyshop.pay.entity.PayRecord;
import com.tinyshop.pay.mapper.PayRecordMapper;
import com.tinyshop.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 支付 Service 实现类
 *
 * @author TinyShop Team
 */
@Slf4j
@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private PayRecordMapper payRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pay(Long orderId, Long userId, BigDecimal amount) {
        // 创建支付记录
        PayRecord record = new PayRecord();
        record.setId(SnowflakeIdUtil.nextId());
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setAmount(amount);
        record.setStatus(0); // 待支付
        record.setChannel("alipay");

        payRecordMapper.insert(record);

        log.info("创建支付记录: payId={}, orderId={}, amount={}", record.getId(), orderId, amount);

        // 模拟调用第三方支付接口
        // 实际项目中会调用支付宝/微信 SDK
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean callback(Long orderId, String tradeNo) {
        PayRecord record = payRecordMapper.selectByOrderId(orderId);
        if (record == null) {
            log.error("支付记录不存在: orderId={}", orderId);
            return false;
        }

        if (record.getStatus() == 1) {
            log.warn("订单已支付，重复回调: orderId={}", orderId);
            return true;
        }

        record.setStatus(1); // 支付成功
        record.setTradeNo(tradeNo);
        payRecordMapper.updateById(record);

        log.info("支付回调成功: orderId={}, tradeNo={}", orderId, tradeNo);
        return true;
    }
}
