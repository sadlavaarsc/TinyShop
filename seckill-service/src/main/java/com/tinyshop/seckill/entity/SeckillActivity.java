package com.tinyshop.seckill.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SeckillActivity {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer version;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public BigDecimal getSeckillPrice() { return seckillPrice; }
    public void setSeckillPrice(BigDecimal seckillPrice) { this.seckillPrice = seckillPrice; }
}
