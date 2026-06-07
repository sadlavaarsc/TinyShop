package com.tinyshop.product.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.result.R;
import com.tinyshop.product.entity.Product;
import com.tinyshop.product.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品 Service
 *
 * @author TinyShop Team
 */
@Slf4j
@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_STOCK_KEY = "product:stock:";
    private static final String PRODUCT_CACHE_KEY = "product:info:";

    @Override
    public Product getById(Long id) {
        // 先查缓存
        String cacheKey = PRODUCT_CACHE_KEY + id;
        Product product = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (product != null) {
            return product;
        }
        // 查数据库
        product = productMapper.selectById(id);
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, 10, TimeUnit.MINUTES);
        }
        return product;
    }

    public List<Product> list(int page, int size) {
        return productMapper.selectPage((page - 1) * size, size);
    }

    public Integer getStock(Long productId) {
        // 先查 Redis 缓存库存
        String stockKey = PRODUCT_STOCK_KEY + productId;
        Object stockObj = redisTemplate.opsForValue().get(stockKey);
        if (stockObj != null) {
            return Integer.parseInt(stockObj.toString());
        }
        // 回源数据库
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(R.Code.PRODUCT_NOT_FOUND, "商品不存在");
        }
        redisTemplate.opsForValue().set(stockKey, product.getStock(), 10, TimeUnit.MINUTES);
        return product.getStock();
    }

    /**
     * 扣减库存（乐观锁）
     */
    public boolean deductStock(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(R.Code.PRODUCT_NOT_FOUND, "商品不存在");
        }
        if (product.getStock() < quantity) {
            return false;
        }
        int affected = productMapper.decreaseStock(productId, quantity, product.getVersion());
        if (affected > 0) {
            // 更新 Redis 缓存
            String stockKey = PRODUCT_STOCK_KEY + productId;
            redisTemplate.opsForValue().decrement(stockKey, quantity);
            log.info("扣减库存成功: productId={}, quantity={}", productId, quantity);
            return true;
        }
        log.warn("扣减库存失败（乐观锁冲突）: productId={}, quantity={}", productId, quantity);
        return false;
    }
}
