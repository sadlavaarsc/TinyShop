package com.tinyshop.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 秒杀服务启动类
 *
 * @author TinyShop Team
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.tinyshop.seckill.mapper")
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
