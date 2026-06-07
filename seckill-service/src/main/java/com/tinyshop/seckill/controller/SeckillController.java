package com.tinyshop.seckill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.tinyshop.common.result.R;
import com.tinyshop.seckill.entity.SeckillActivity;
import com.tinyshop.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀 Controller
 *
 * @author TinyShop Team
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 秒杀活动列表
     */
    @GetMapping("/activities")
    public R<List<SeckillActivity>> listActivities() {
        List<SeckillActivity> list = seckillService.listActivities();
        return R.ok(list);
    }

    /**
     * 秒杀下单接口
     * Sentinel 限流保护
     */
    @PostMapping("/order")
    @SentinelResource(value = "seckillOrder", blockHandler = "seckillOrderBlockHandler")
    public R<String> seckillOrder(
            @RequestParam Long activityId,
            @RequestParam Long userId) {
        boolean success = seckillService.seckillOrder(activityId, userId);
        if (success) {
            return R.ok("秒杀成功，订单处理中");
        }
        return R.error(R.Code.SECKILL_STOCK_EMPTY, "秒杀失败，库存不足");
    }

    /**
     * 秒杀限流降级处理
     */
    public R<String> seckillOrderBlockHandler(Long activityId, Long userId, BlockException ex) {
        log.warn("秒杀接口被限流: activityId={}, userId={}", activityId, userId);
        return R.error(R.Code.SECKILL_RATE_LIMIT, "系统繁忙，请稍后重试");
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("seckill-service is running");
    }
}
