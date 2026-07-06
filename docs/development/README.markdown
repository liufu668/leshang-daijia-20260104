NEW_FILE_CODE
# 乐尚代驾平台 - 微服务项目

## 📖 项目简介

乐尚代驾是一个基于 Spring Cloud Alibaba 的分布式微服务代驾平台，实现了从乘客下单、司机抢单、行程追踪到支付分账的完整业务流程。

**技术定位**：高并发场景下的分布式系统实战项目

**核心亮点**：
- 🚀 高并发抢单：Redis 分布式锁 + Lua 脚本 + RabbitMQ 死信队列三重保障
- 💰 分布式事务：Seata AT 模式保证支付后数据一致性
- 📍 实时定位：MongoDB GeoHash 附近司机搜索 + 轨迹追踪
- 🎯 动态计费：Drools 规则引擎支持灵活配置费用规则
- ⏰ 分布式调度：XXL-Job 实现订单超时取消、死信消息重试

---

## 🏗️ 技术架构

### 技术栈

| 分类 | 技术选型 | 版本 |
|------|---------|------|
| **开发语言** | Java | 17 |
| **应用框架** | Spring Boot | 3.2.4 |
| **微服务治理** | Spring Cloud | 2023.0.1 |
| **服务注册/配置** | Nacos | 2023.0.1.0 |
| **服务调用** | OpenFeign | - |
| **熔断降级** | Sentinel | - |
| **API 网关** | Spring Cloud Gateway | - |
| **关系数据库** | MySQL | 8.3.0 |
| **文档数据库** | MongoDB | - |
| **缓存** | Redis + Redisson | 3.27.2 |
| **消息队列** | RabbitMQ | - |
| **分布式事务** | Seata | 1.7.0 |
| **任务调度** | XXL-Job | 2.4.0 |
| **规则引擎** | Drools | 8.41.0.Final |
| **对象存储** | MinIO | 8.5.2 |
| **API 文档** | Knife4j | 4.1.0 |

### 模块架构

leshang-daijia-20260104 (父工程) 
├── common/ # 公共模块 
│ ├── common-util/ # 通用工具类 
│ ├── rabbit-util/ # RabbitMQ 封装 
│ ├── service-security/ # 安全认证 
│ └── service-util/ # 服务配置 
├── model/ # 数据模型 
├── server-gateway/ # API 网关 (8600) 
├── service/ # 业务服务层 
│ ├── service-customer/ # 乘客服务 (8501) 
│ ├── service-driver/ # 司机服务 (8502) 
│ ├── service-map/ # 地图服务 (8503) 
│ ├── service-rules/ # 规则引擎 (8504) 
│ ├── service-order/ # 订单服务 (8505) 
│ ├── service-payment/ # 支付服务 (8506) 
│ ├── service-dispatch/ # 调度服务 (8509) 
│ └── service-coupon/ # 优惠券 (8511) 
├── service-client/ # Feign 客户端 
├── web/ # Web 接入层 
│ ├── web-customer/ # 乘客端 (8601) 
│ └── web-driver/ # 司机端 (8602) 
└── xxl-job-master/ # 分布式任务调度

---

## 🎯 核心功能

### 1. 乘客端业务流程

登录 → 输入起点终点 → 预估费用 → 下单 → 等待接单 → 司机到达 → 开始行程 → 结束行程 → 支付 → 评价

### 2. 司机端业务流程

登录 → 听单模式 → 抢单 → 联系乘客 → 接驾 → 开始行程 → 结束行程 → 收款 → 提现

### 3. 高并发抢单解决方案

**问题**：多个司机同时抢一个订单，如何防止超卖？

**三层保障**：
1. **Redis 分布式锁**：`RLock` 控制并发访问
2. **Lua 原子脚本**：判断订单状态原子操作
3. **数据库乐观锁**：`version` 字段最终兜底

**核心代码位置**：
- `service-order/src/main/java/com/daijia/order/service/impl/OrderInfoServiceImpl.java`

### 4. 消息可靠性保障

**RabbitMQ 死信队列机制**：
- 主队列 TTL：10 分钟
- 消费失败 → 死信队列 → 定时任务重试
- 发布确认 + 手动 ACK + 本地重试

**核心代码位置**：
- `common/service-util/src/main/java/com/daijia/common/config/rabbitmq/RabbitConfig.java`
- `service-order/src/main/java/com/daijia/order/consumer/OrderRobConsumer.java`

### 5. 分布式事务

**场景**：支付成功后需要更新订单状态、司机账户、分账记录

**方案**：Seata AT 模式
- TM：`web-customer` 发起全局事务
- RM：`service-order`、`service-payment`、`service-driver` 参与

**核心代码位置**：
- `service-payment/src/main/java/com/daijia/payment/service/impl/WxPayServiceImpl.java`

---

## 🚀 快速启动

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- MongoDB 5.0+
- RabbitMQ 3.8+
- Nacos 2.2+

### 启动步骤

1. **启动中间件**

按顺序启动
MySQL → Redis → MongoDB → RabbitMQ → Nacos → XXL-Job

2. **导入数据库**

```sql
-- 执行 SQL 脚本 
daijia.sql 
xxl-job-master/doc/db/tables_xxl-job.sql
```

3. **配置 Nacos**
- 访问 http://localhost:8848/nacos
- 用户名/密码：nacos/nacos
- 上传 DEFAULT_GROUP 目录下的配置文件

4. **启动服务**

按依赖顺序启动
mvn clean package -Dmaven.test.skip=true
依次启动
server-gateway 
service-customer 
service-driver 
service-order 
service-map 
service-rules 
service-dispatch 
service-payment 
service-coupon 
web-customer 
web-driver

5. **访问 API 文档**

http://localhost:8600/doc.html

---

## 📝 关键技术实现

### 1. Redis 分布式锁

```java
RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId); 
try { 
    boolean flag = lock.tryLock(0, RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.MILLISECONDS ); 
    if(flag) { 
         //执行业务逻辑 
    } 
} finally { 
    if (lock.isHeldByCurrentThread()) { 
        lock.unlock(); 
    } 
}
```

### 2. Lua 原子脚本

```java
RedisScript<Long> existsScript = new DefaultRedisScript<>( 
        "if redis.call('exists', KEYS[1]) == 1 then return 1 else return 0 end", 
        Long.class ); 
Long exists = redisTemplate.execute(existsScript, Collections.singletonList(key));
```

### 3. RabbitMQ 死信队列

```java
@Bean public Queue queue() { 
    Map<String, Object> args = new HashMap<>(4); 
    args.put("x-dead-letter-exchange", DLQ_EXCHANGE); 
    args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY); 
    args.put("x-message-ttl", 10 * 60 * 1000); 
    args.put("x-max-length", 2000); 
    return new Queue(QUEUE, true, false, false, args); 
}
```

### 4. Seata 分布式事务

```java
@GlobalTransactional 
public void handleOrder(String orderNo) { 
    orderInfoFeignClient.updateOrderPayStatus(orderNo); 
    driverAccountFeignClient.transfer(transferForm); 
}
```

---

## 🔧 项目优化方向

### 已完成
- ✅ 高并发抢单机制
- ✅ 消息可靠性保障
- ✅ 分布式事务一致性
- ✅ 规则引擎动态计费

### 待优化
- 🔄 补充单元测试（目标覆盖率 80%）
- 🔄 完善 JavaDoc 注释
- 🔄 引入 SonarQube 代码质量检查
- 🔄 添加接口限流和熔断策略
- 🔄 优化日志输出和链路追踪

---

## 📚 学习收获

通过这个项目，我深入理解了：
1. 微服务架构的设计原则和拆分策略
2. 高并发场景下的数据一致性解决方案
3. 分布式系统的常见问题（事务、锁、消息可靠性）
4. 中间件的实际应用场景和调优技巧
5. 工程化思维和代码规范的重要性

---

## 👨‍💻 联系方式

如有问题，欢迎交流！
