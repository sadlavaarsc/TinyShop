package com.tinyshop.pay.controller;

import com.tinyshop.common.result.R;
import com.tinyshop.pay.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/pay")
public class PayController {
    @Autowired
    private PayService payService;

    @PostMapping("/do")
    public R<Boolean> pay(@RequestParam Long orderId,
                            @RequestParam Long userId,
                            @RequestParam BigDecimal amount) {
        return R.ok(payService.pay(orderId, userId, amount));
    }

    @PostMapping("/callback")
    public R<Boolean> callback(@RequestParam Long orderId, @RequestParam String tradeNo) {
        return R.ok(payService.callback(orderId, tradeNo));
    }
}
