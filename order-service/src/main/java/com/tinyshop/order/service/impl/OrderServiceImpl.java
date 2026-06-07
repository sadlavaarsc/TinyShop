package com.tinyshop.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.result.R;
import com.tinyshop.common.utils.SnowflakeIdUtil;
import com.tinyshop.order.entity.Order;
import com.tinyshop.order.entity.OrderItem;
import com.tinyshop.order.feign.ProductFeignClient;
import com.tinyshop.order.mapper.OrderItemMapper;
import com.tinyshop.order.mapper.OrderMapper;
import com.tinyshop.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单 Service 实现类
 *
 * @author TinyShop Team
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 创建订单（Seata AT 分布式事务）
     */
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public Long createOrder(Long userId, Long productId, Integer quantity) {
        // 1. 查询商品信息（通过 Feign 调用商品服务）
        R<?> productResult = productFeignClient.getProduct(productId);
        if (productResult.getCode() != 200 || productResult.getData() == null) {
            throw new BusinessException(R.Code.PRODUCT_NOT_FOUND, "商品不存在");
        }

        // 模拟获取商品价格和库存（实际应从 Feign 返回的 DTO 中获取）
        BigDecimal productPrice = new BigDecimal("99.00");
        BigDecimal totalAmount = productPrice.multiply(new BigDecimal(quantity));

        // 2. 扣减库存（通过 Feign 调用商品服务）
        R<Boolean> deductResult = productFeignClient.deductStock(productId, quantity);
        if (deductResult.getCode() != 200 || !Boolean.TRUE.equals(deductResult.getData())) {
            throw new BusinessException(R.Code.PRODUCT_STOCK_NOT_ENOUGH, "库存不足");
        }

        // 3. 生成订单ID（雪花算法）
        Long orderId = SnowflakeIdUtil.nextId();

        // 4. 创建订单
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setAddress("默认收货地址");
        order.setReceiver("默认收件人");
        order.setPhone("13800138000");
        orderMapper.insert(order);

        // 5. 创建订单项
        OrderItem orderItem = new OrderItem();
        orderItem.setId(SnowflakeIdUtil.nextId());
        orderItem.setOrderId(orderId);
        orderItem.setProductId(productId);
        orderItem.setProductName("示例商品");
        orderItem.setProductPrice(productPrice);
        orderItem.setQuantity(quantity);
        orderItem.setSubtotal(totalAmount);
        orderItemMapper.insert(orderItem);

        log.info("订单创建成功: orderId={}, userId={}, productId={}, quantity={}",
                orderId, userId, productId, quantity);

        return orderId;
    }

    @Override
    public List<Order> listByUserId(Long userId) {
        return orderMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(R.Code.ORDER_NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(R.Code.BAD_REQUEST, "只能取消待支付订单");
        }

        order.setStatus(4); // 已取消
        orderMapper.updateById(order);

        log.info("订单取消成功: orderId={}", orderId);
    }
}
