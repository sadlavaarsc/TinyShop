package com.tinyshop.pay.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.result.R;
import com.tinyshop.common.utils.SnowflakeIdUtil;
import com.tinyshop.pay.entity.PayRecord;
import com.tinyshop.pay.mapper.PayRecordMapper;
import com.tinyshop.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    /**
     * 模拟支付
     * 实际项目中这里会调用微信支付/支付宝等第三方支付接口
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pay(Long orderId, Long userId, BigDecimal amount) {
        // 1. 检查是否已支付
        PayRecord existRecord = payRecordMapper.selectByOrderId(orderId);
        if (existRecord != null && existRecord.getStatus() == 1) {
            throw new BusinessException(R.Code.BAD_REQUEST, "订单已支付");
        }

        // 2. 创建支付记录
        PayRecord record = new PayRecord();
        record.setId(SnowflakeIdUtil.nextId());
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setAmount(amount);
        record.setStatus(0); // 支付中
        record.setPayChannel("MOCK_PAY"); // 模拟支付渠道
        record.setCreateTime(LocalDateTime.now());

        payRecordMapper.insert(record);

        // 3. 模拟调用第三方支付（实际应调用真实支付接口）
        log.info("模拟支付请求: orderId={}, userId={}, amount={}", orderId, userId, amount);

        // 模拟支付成功
        record.setStatus(1); // 已支付
        record.setTradeNo("MOCK" + System.currentTimeMillis());
        record.setPayTime(LocalDateTime.now());
        payRecordMapper.updateById(record);

        log.info("模拟支付成功: orderId={}, tradeNo={}", orderId, record.getTradeNo());
        return true;
    }

    /**
     * 模拟支付回调
     * 实际项目中由第三方支付平台回调此接口
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean callback(Long orderId, String tradeNo) {
        PayRecord record = payRecordMapper.selectByOrderId(orderId);
        if (record == null) {
            throw new BusinessException(R.Code.ORDER_NOT_FOUND, "支付记录不存在");
        }

        if (record.getStatus() == 1) {
            log.info("支付回调重复处理: orderId={}", orderId);
            return true;
        }

        // 更新支付状态
        record.setStatus(1);
        record.setTradeNo(tradeNo);
        record.setPayTime(LocalDateTime.now());
        payRecordMapper.updateById(record);

        log.info("支付回调处理成功: orderId={}, tradeNo={}", orderId, tradeNo);
        return true;
    }
}
