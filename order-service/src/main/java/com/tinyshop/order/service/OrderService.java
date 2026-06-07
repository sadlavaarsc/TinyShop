package com.tinyshop.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tinyshop.order.entity.Order;

import java.util.List;

/**
 * 订单 Service 接口
 *
 * @author TinyShop Team
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单（分布式事务）
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  数量
     * @return 订单ID
     */
    Long createOrder(Long userId, Long productId, Integer quantity);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> listByUserId(Long userId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId);
}
