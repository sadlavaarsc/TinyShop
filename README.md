# TinyShop - 高并发电商系统

## 项目简介

TinyShop 是一个基于微服务架构的高并发电商系统，采用 Java + Spring Boot 技术栈，集成了分布式事务、消息队列、缓存、限流、分库分表等企业级中间件。

## 系统架构

```
                    +------------------+
                    |     客户端        |
                    +--------+---------+
                             |
                    +--------v---------+
                    |  Nginx / CDN     |
                    +--------+---------+
                             |
              +--------------v--------------+
              |   Spring Cloud Gateway      |
              |   (路由 / 限流 / 鉴权)       |
              +--------------+--------------+
                             |
        +--------------------+--------------------+
        |                    |                    |
+-------v-------+   +--------v--------+   +------v------+
|  user-service |   | product-service |   | order-service|
|   用户服务     |   |    商品服务      |   |   订单服务   |
+-------+-------+   +--------+--------+   +------+------+
        |                    |                    |
        +--------------------+--------------------+
                             |
                    +--------v---------+
                    |  seckill-service |
                    |    秒杀服务       |
                    +--------+---------+
                             |
                    +--------v---------+
                    |   pay-service    |
                    |    支付服务       |
                    +------------------+

## 技术中间件

+--------------------------------------------------+
|                    基础设施层                      |
|  Nacos (注册中心/配置中心)                          |
|  Sentinel (限流熔断)                              |
|  Seata (分布式事务)                                |
+--------------------------------------------------+
|                    数据存储层                      |
|  MySQL (主从复制)                                  |
|  Redis (缓存/分布式锁)                             |
|  RocketMQ (异步消息)                               |
+--------------------------------------------------+
```

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.x | 基础框架 |
| Spring Cloud Gateway | 2021.0.x | API 网关 |
| MyBatis Plus | 3.5.x | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 6.x | 缓存 / 分布式锁 |
| Redisson | 3.23.x | Redis 客户端 |
| RocketMQ | 4.9.x | 消息队列 |
| Sentinel | 1.8.x | 限流熔断 |
| Seata | 1.7.x | 分布式事务 |
| ShardingSphere | 5.4.x | 分库分表 |
| Nacos | 2.x | 注册中心 / 配置中心 |
| JWT | 4.4.x | 身份认证 |

## 模块说明

### gateway（网关模块）
- 统一入口，路由转发到各微服务
- 集成 Sentinel 网关限流
- 负载均衡

### common（公共模块）
- 统一响应封装 `R<T>`
- 全局异常处理
- JWT 工具类
- 雪花算法 ID 生成器
- Redis 工具类

### user-service（用户服务）
- 用户注册 / 登录
- JWT Token 签发与验证
- 密码 MD5 + 盐值加密

### product-service（商品服务）
- 商品列表 / 详情
- 库存查询 / 扣减
- 乐观锁防止超卖
- Redis 缓存商品信息

### order-service（订单服务）
- 创建订单（Seata 分布式事务）
- 订单列表 / 详情
- ShardingSphere 分库分表（16 表分片）
- OpenFeign 调用商品服务

### seckill-service（秒杀服务）
- 秒杀活动列表
- **Redis Lua 脚本原子扣减库存**
- **Sentinel 注解限流**
- **RocketMQ 异步下单**
- **MySQL 乐观锁兜底**

### pay-service（支付服务）
- 模拟支付接口
- 模拟支付回调
- 支付记录持久化

## 核心设计亮点

### 1. 秒杀系统高并发设计

```
用户请求 -> Gateway 限流 -> Sentinel 注解限流
                |
                v
    Redis Lua 原子脚本扣减库存
                |
        +-------+-------+
        | 扣减成功       | 扣减失败
        v               v
   发送 MQ 消息      返回失败
        |
        v
   异步创建订单
        |
        v
   MySQL 乐观锁兜底
```

- **Redis 预减库存**：活动开始前将库存加载到 Redis，利用 Lua 脚本保证原子性
- **限流保护**：Gateway + Sentinel 双层限流
- **异步削峰**：RocketMQ 异步处理下单，降低数据库压力
- **防重购买**：Redis 标记用户已购状态
- **MySQL 兜底**：乐观锁 version 字段防止超卖

### 2. 分布式事务（Seata AT 模式）

订单创建涉及多个服务：
- 商品服务扣减库存
- 订单服务创建订单
- 使用 `@GlobalTransactional` 保证数据一致性

### 3. 分库分表（ShardingSphere）

订单表按 `user_id` 分库分表：
- 2 个数据库：ds0, ds1
- 每个库 16 张表：t_order_0 ~ t_order_15
- 路由规则：`user_id % 2` 选库，`user_id % 16` 选表

### 4. 缓存策略

- 商品信息：Redis 缓存 30 分钟
- 商品库存：Redis 缓存 10 分钟
- 用户 Token：Redis 缓存 7 天

## 数据库设计

### 用户库 (tinyshop_user)
```sql
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(64) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 商品库 (tinyshop_product)
```sql
CREATE TABLE t_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    image VARCHAR(255) COMMENT '商品图片',
    category_id BIGINT COMMENT '分类ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```

### 订单库 (tinyshop_order_0 / tinyshop_order_1)
```sql
-- 每个库创建 16 张分表：t_order_0 ~ t_order_15
CREATE TABLE t_order_0 (
    id BIGINT PRIMARY KEY COMMENT '订单ID（雪花算法）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
    address VARCHAR(255) COMMENT '收货地址',
    receiver VARCHAR(64) COMMENT '收货人',
    phone VARCHAR(20) COMMENT '手机号',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

### 秒杀库 (tinyshop_seckill)
```sql
CREATE TABLE t_seckill_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) COMMENT '活动名称',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock INT NOT NULL COMMENT '秒杀库存',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT DEFAULT 0 COMMENT '状态：0-未开始 1-进行中 2-已结束',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';
```

### 支付库 (tinyshop_pay)
```sql
CREATE TABLE t_pay_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待支付 1-成功 2-失败',
    trade_no VARCHAR(64) COMMENT '第三方流水号',
    channel VARCHAR(32) COMMENT '支付渠道',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';
```

## 接口文档

### 用户服务 (user-service:8081)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/user/register` | POST | 用户注册 |
| `/user/login` | POST | 用户登录 |
| `/user/{userId}` | GET | 查询用户信息 |

### 商品服务 (product-service:8082)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/product/list` | GET | 商品列表 |
| `/product/{productId}` | GET | 商品详情 |
| `/product/stock/{productId}` | GET | 查询库存 |
| `/product/deductStock` | POST | 扣减库存 |

### 订单服务 (order-service:8083)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/order/create` | POST | 创建订单 |
| `/order/list` | GET | 订单列表 |
| `/order/{orderId}` | GET | 订单详情 |
| `/order/cancel/{orderId}` | POST | 取消订单 |

### 秒杀服务 (seckill-service:8084)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/seckill/activities` | GET | 秒杀活动列表 |
| `/seckill/order` | POST | 秒杀下单 |

### 支付服务 (pay-service:8085)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/pay/do` | POST | 模拟支付 |
| `/pay/callback` | POST | 支付回调 |

## 启动说明

### 1. 启动基础设施
```bash
# Nacos
sh startup.sh -m standalone

# Redis
redis-server

# RocketMQ
sh mqnamesrv
sh mqbroker -n localhost:9876

# MySQL
# 创建数据库并执行上述建表语句
```

### 2. 初始化秒杀库存
```bash
# 活动开始前，将库存预热到 Redis
curl -X POST "http://localhost:8084/seckill/initStock?activityId=1"
```

### 3. 按顺序启动服务
```bash
# 1. 用户服务
cd user-service && mvn spring-boot:run

# 2. 商品服务
cd product-service && mvn spring-boot:run

# 3. 订单服务
cd order-service && mvn spring-boot:run

# 4. 秒杀服务
cd seckill-service && mvn spring-boot:run

# 5. 支付服务
cd pay-service && mvn spring-boot:run

# 6. 网关
cd gateway && mvn spring-boot:run
```

### 4. 通过网关访问
```bash
# 用户注册
curl -X POST "http://localhost:8080/api/user/register?username=test&password=123456&phone=13800138000"

# 用户登录
curl -X POST "http://localhost:8080/api/user/login?username=test&password=123456"

# 商品列表
curl "http://localhost:8080/api/product/list"

# 秒杀下单
curl -X POST "http://localhost:8080/api/seckill/order?activityId=1&userId=1"
```

## 项目结构

```
TinyShop/
├── README.md
├── pom.xml                           # 父 POM
├── gateway/                          # Spring Cloud Gateway 网关
├── common/                           # 公共模块（工具类、响应封装、异常处理）
├── user-service/                     # 用户服务（注册/登录/JWT）
├── product-service/                  # 商品服务（列表/详情/库存/乐观锁）
├── order-service/                    # 订单服务（创建/列表/分库分表/分布式事务）
├── seckill-service/                  # 秒杀服务（Lua/限流/MQ/乐观锁）
└── pay-service/                      # 支付服务（模拟支付/回调）
```

## 作者

TinyShop Team
