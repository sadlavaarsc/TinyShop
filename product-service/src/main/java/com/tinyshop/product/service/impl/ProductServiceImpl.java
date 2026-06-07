package com.tinyshop.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.result.R;
import com.tinyshop.product.entity.Product;
import com.tinyshop.product.mapper.ProductMapper;
import com.tinyshop.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品 Service 实现类
 *
 * @author TinyShop Team
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 商品缓存前缀 */
    private static final String PRODUCT_CACHE_PREFIX = "product:";

    /** 库存缓存前缀 */
    private static final String STOCK_CACHE_PREFIX = "product:stock:";

    @Override
    public List<Product> listProducts(Integer page, Integer size) {
        Page<Product> pageParam = new Page<>(page, size);
        return this.page(pageParam).getRecords();
    }

    @Override
    public Product getProductDetail(Long productId) {
        // 先查缓存
        String cacheKey = PRODUCT_CACHE_PREFIX + productId;
        Product product = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (product != null) {
            return product;
        }

        // 查数据库
        product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(R.Code.PRODUCT_NOT_FOUND, "商品不存在");
        }

        // 写入缓存，设置30分钟过期
        redisTemplate.opsForValue().set(cacheKey, product, 30, TimeUnit.MINUTES);
        return product;
    }

    @Override
    public Integer getStock(Long productId) {
        // 先查缓存
        String cacheKey = STOCK_CACHE_PREFIX + productId;
        Object stockObj = redisTemplate.opsForValue().get(cacheKey);
        if (stockObj != null) {
            return Integer.parseInt(stockObj.toString());
        }

        // 查数据库
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(R.Code.PRODUCT_NOT_FOUND, "商品不存在");
        }

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, product.getStock(), 10, TimeUnit.MINUTES);
        return product.getStock();
    }

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(R.Code.PRODUCT_NOT_FOUND, "商品不存在");
        }

        int affected = productMapper.deductStock(productId, quantity, product.getVersion());
        if (affected <= 0) {
            log.warn("扣减库存失败，可能库存不足或并发冲突: productId={}", productId);
            return false;
        }

        // 更新缓存库存
        String cacheKey = STOCK_CACHE_PREFIX + productId;
        redisTemplate.opsForValue().decrement(cacheKey, quantity);

        log.info("扣减库存成功: productId={}, quantity={}", productId, quantity);
        return true;
    }
}
