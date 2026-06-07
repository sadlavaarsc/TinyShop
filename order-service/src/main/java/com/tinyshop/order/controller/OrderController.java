package com.tinyshop.order.controller;

import com.tinyshop.common.result.R;
import com.tinyshop.order.entity.Order;
import com.tinyshop.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 Controller
 *
 * @author TinyShop Team
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public R<Long> createOrder(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        Long orderId = orderService.createOrder(userId, productId, quantity);
        return R.ok("订单创建成功", orderId);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    public R<Order> getOrder(@PathVariable Long orderId) {
        Order order = orderService.getById(orderId);
        return R.ok(order);
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/list")
    public R<List<Order>> listByUserId(@RequestParam Long userId) {
        List<Order> list = orderService.listByUserId(userId);
        return R.ok(list);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderId}")
    public R<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return R.ok("订单已取消");
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("order-service is running");
    }
}
