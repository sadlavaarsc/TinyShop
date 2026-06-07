package com.tinyshop.product.controller;

import com.tinyshop.common.result.R;
import com.tinyshop.product.entity.Product;
import com.tinyshop.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品 Controller
 *
 * @author TinyShop Team
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 商品详情
     */
    @GetMapping("/{productId}")
    public R<Product> getById(@PathVariable Long productId) {
        Product product = productService.getById(productId);
        return R.ok(product);
    }

    /**
     * 商品列表
     */
    @GetMapping("/list")
    public R<List<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(productService.list(page, size));
    }

    /**
     * 查询库存
     */
    @GetMapping("/stock/{productId}")
    public R<Integer> getStock(@PathVariable Long productId) {
        Integer stock = productService.getStock(productId);
        return R.ok(stock);
    }

    /**
     * 扣减库存（供订单服务 Feign 调用）
     */
    @PostMapping("/deductStock")
    public R<Boolean> deductStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        boolean success = productService.deductStock(productId, quantity);
        return R.ok(success);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("product-service is running");
    }
}
