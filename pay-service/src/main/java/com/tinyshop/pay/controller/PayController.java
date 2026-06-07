package com.tinyshop.pay.controller;

import com.tinyshop.common.result.R;
import com.tinyshop.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付 Controller
 *
 * @author TinyShop Team
 */
@Slf4j
@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayService payService;

    /**
     * 模拟支付
     */
    @PostMapping("/do")
    public R<Boolean> pay(@RequestParam Long orderId,
                          @RequestParam Long userId,
                          @RequestParam BigDecimal amount) {
        boolean success = payService.pay(orderId, userId, amount);
        return R.ok("支付请求已提交", success);
    }

    /**
     * 模拟支付回调
     */
    @PostMapping("/callback")
    public R<Boolean> callback(@RequestParam Long orderId,
                               @RequestParam String tradeNo) {
        boolean success = payService.callback(orderId, tradeNo);
        return R.ok("回调处理完成", success);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("pay-service is running");
    }
}
