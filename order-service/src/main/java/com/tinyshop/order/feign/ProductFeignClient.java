package com.tinyshop.order.feign;

import com.tinyshop.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务 Feign 客户端
 *
 * @author TinyShop Team
 */
@FeignClient(name = "product-service", fallback = ProductFeignClientFallback.class)
public interface ProductFeignClient {

    /**
     * 查询商品详情
     */
    @GetMapping("/product/{productId}")
    R<?> getProduct(@PathVariable("productId") Long productId);

    /**
     * 扣减库存
     */
    @PostMapping("/product/deductStock")
    R<Boolean> deductStock(@RequestParam("productId") Long productId,
                             @RequestParam("quantity") Integer quantity);
}
