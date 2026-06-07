package com.tinyshop.order.feign;

import com.tinyshop.common.result.R;
import org.springframework.stereotype.Component;

/**
 * 商品服务 Feign 降级处理
 *
 * @author TinyShop Team
 */
@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    @Override
    public R<?> getProduct(Long productId) {
        return R.error(R.Code.SERVICE_UNAVAILABLE, "商品服务暂不可用，请稍后重试");
    }

    @Override
    public R<Boolean> deductStock(Long productId, Integer quantity) {
        return R.error(R.Code.SERVICE_UNAVAILABLE, "商品服务暂不可用，请稍后重试");
    }
}
