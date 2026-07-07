# 乐尚代驾平台 - 微服务项目
<p align="center">
  <strong>基于 Spring Cloud Alibaba 的分布式微服务代驾平台</strong>
</p>

<p align="center">
  <a href="#-项目简介">项目简介</a> •
  <a href="#-技术架构">技术架构</a> •
  <a href="#-核心功能">核心功能</a> •
  <a href="#-快速启动">快速启动</a> •
  <a href="#-关键技术实现">关键技术实现</a> •
  <a href="#-数据库与端口规划">数据库与端口规划</a> •
  <a href="#-项目优化与学习收获">项目优化与学习收获</a>
</p>

---

## 📖 项目简介
**乐尚代驾**是完整O2O代驾服务微服务平台，采用Spring Cloud Alibaba分布式架构，覆盖乘客下单、司机抢单、行程导航、支付核销、优惠券管理全业务链路，针对高并发、数据一致性、接口性能做大量实战优化，是面向后端面试的综合实战项目。

### 项目定位
落地高并发分布式场景真实技术难题，完整覆盖缓存、消息队列、分布式锁、分布式事务、限流监控、地理检索等主流后端技术栈。

### 核心亮点
- 🚀 **高并发抢单**：Lua原子脚本+Redis分布式锁+MySQL乐观锁三层防超卖，RabbitMQ异步削峰
- 🛡️ **消息可靠保障**：生产者Confirm、手动ACK、死信队列、定时巡检兜底，零消息丢失
- 📍 **地理位置检索**：Redis GEO实时司机位置检索，MongoDB持久化海量行驶轨迹
- 🎫 **优惠券安全机制**：预占锁定+双机制自动回收，避免优惠券永久占用损失
- ⚡ **接口性能优化**：异步线程池、批量Feign调用、多级缓存，QPS与响应耗时大幅优化
- 📈 **微服务高可用**：Gateway+Sentinel双层限流熔断，防止服务雪崩
- 📊 **全链路监控**：Actuator+Prometheus+Grafana统一指标可视化告警

---

## 🏗️ 技术架构
### 完整技术栈
| 分类 | 技术选型 |
|------|---------|
| 开发语言 | Java |
| 基础框架 | SpringBoot、SpringCloud、SpringCloud Gateway、OpenFeign、Sentinel |
| 注册配置中心 | Nacos |
| 持久层 | MyBatis-Plus、MySQL 8.0 |
| 缓存 & 分布式锁 | Redis、Redisson |
| 消息队列 | RabbitMQ |
| 文档数据库 | MongoDB |
| 分布式事务 | Seata AT模式 |
| 定时任务 | XXL-Job |
| API文档 | Knife4j（访问地址：http://localhost:8600/doc.html） |
| 监控体系 | SpringBoot Actuator、Prometheus、Grafana |
| 版本管理 | Git、Gitee/GitHub |

### 项目模块分层结构
```
leshang-daijia-20260104  # Maven父工程
├── common/                # 公共依赖模块
│   ├── common-util/       # 通用工具类、常量、异常封装
│   ├── rabbit-util/       # RabbitMQ统一配置封装
│   ├── service-security/  # 登录鉴权工具
│   └── service-util/     # 微服务通用配置
├── model/                 # 统一实体、DTO、VO数据模型
├── server-gateway/        # API网关 端口8600
├── service/               # 核心业务微服务
│   ├── service-customer/  # 乘客基础服务 8501
│   ├── service-driver/    # 司机服务 8502
│   ├── service-map/       # 地图位置服务 8503
│   ├── service-rules/     # 计费规则服务 8504
│   ├── service-order/     # 订单核心服务 8505
│   ├── service-payment/   # 支付服务 8506
│   ├── service-dispatch/  # 派单调度服务 8509
│   └── service-coupon/    # 优惠券服务 8511
├── service-client/        # Feign远程调用客户端
├── web/                   # 前端接入层
│   ├── web-customer/      # 乘客小程序接口 8601
│   └── web-driver/        # 司机小程序接口 8602
└── xxl-job-master/        # 分布式定时任务调度中心
```

---

## 🎯 核心功能与业务解决方案
### 1. 乘客端完整流程
1. 用户登录 → 输入起止地址预估费用
2. 下单存入Redis缓存，异步派单解耦
3. 等待司机抢单，实时查看司机位置
4. 司机接驾、行程开始、行程结束
5. 微信支付核销优惠券、订单评价

### 2. 司机端完整流程
1. 司机登录开启听单模式
2. 周边订单实时推送、并发抢单
3. 联系乘客、前往起点接驾
4. 行程结束自动计费、收款
5. 账户余额提现

### 3. 高并发抢单核心解决方案
**业务问题**：多司机同时争抢同一订单，出现重复接单、超卖、数据不一致
三层并发安全保障：
1. Lua原子脚本：单条Redis命令完成订单状态判断+抢单标记写入，避免`HEXISTS+HSET`两段式并发漏洞
2. Redisson分布式锁：热点订单并发限流控制
3. MySQL乐观锁version：数据库层最终兜底，更新失败直接丢弃重复请求

**代码位置**：`service-order/src/main/java/com/daijia/order/service/impl/OrderInfoServiceImpl.java`

### 4. RabbitMQ消息可靠投递（抢单异步削峰）
1. 生产者开启Publisher-Confirm机制，确保消息投递成功
2. 交换机、队列、消息全部持久化，服务重启不丢失消息
3. 消费者关闭自动ACK，业务处理完成手动确认
4. 配置死信队列：消息重试耗尽/过期转入死信，定时任务巡检修复异常订单
5. 大量抢单请求异步落库，削峰避免瞬时流量打垮MySQL

**代码位置**
- 队列配置：`common/service-util/src/main/java/com/daijia/common/config/rabbitmq/RabbitConfig.java`
- 消息消费：`service-order/src/main/java/com/daijia/order/consumer/OrderRobConsumer.java`

### 5. 支付优惠券预占安全机制
问题：未支付成功直接核销优惠券，支付失败无法回退造成券损失
解决方案：
1. 下单预占锁定优惠券，标记不可使用
2. 支付成功正式核销；支付超时/用户主动取消，两套机制回收优惠券
3. XXL-Job定时关单任务兜底，防止服务宕机导致优惠券永久锁定

### 6. 分布式事务（Seata AT）
场景：支付成功后需同步更新订单状态、司机账户余额、分账记录，多服务同时操作需保证原子性
- TM事务发起方：web-customer
- RM参与服务：订单、支付、司机账户
- 一阶段提交记录undo日志，二阶段统一提交/回滚，保证跨库数据一致

**代码位置**：`service-payment/src/main/java/com/daijia/payment/service/impl/WxPayServiceImpl.java`

### 7. 司机地理位置检索优化
1. Redis GEO存储司机实时在线坐标，毫秒级查询周边司机
2. 定时批量同步位置数据至MongoDB持久化行驶轨迹
3. 原循环Feign多次调用改为批量查询+本地内存过滤，接口RT从2.5s降至200ms

### 8. 微服务雪崩防护（Sentinel）
1. Gateway网关层全局限流、黑名单拦截
2. 各个业务服务内部差异化接口限流阈值
3. Feign远程调用配置熔断降级，服务异常返回兜底数据
4. 多层防护避免下游故障连锁拖垮整个集群

---

## 🚀 快速启动指南
### 环境前置要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- MongoDB 5.0+
- RabbitMQ 3.8+
- Nacos 2.2+
- XXL-Job

### 1. 启动中间件顺序
MySQL → Redis → MongoDB → RabbitMQ → Nacos → XXL-Job

### 2. 初始化数据库
执行项目根目录SQL脚本 `daijia.sql`，初始化全部业务库表；
执行xxl-job内置sql初始化定时任务库。

### 3. Nacos配置导入
1. 访问地址：http://localhost:8848/nacos
2. 账号密码：nacos / nacos
3. 上传项目内 `DEFAULT_GROUP` 配置文件，对应各个微服务配置

### 4. 项目编译打包
```bash
mvn clean package -Dmaven.test.skip=true
```

### 5. 服务启动顺序（按依赖由底层到上层）
1. server-gateway 网关
2. service-customer、service-driver、service-map、service-rules、service-order、service-payment、service-coupon、service-dispatch
3. web-customer、web-driver

### 6. 接口文档访问
浏览器打开：http://localhost:8600/doc.html

---

## 🔧 关键技术代码示例
### 1. Redisson分布式锁（抢单防击穿）
```java
RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
try {
    boolean flag = lock.tryLock(0, RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.MILLISECONDS);
    if(flag) {
        // 执行抢单业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

### 2. Redis Lua原子脚本（并发抢单校验）
```java
RedisScript<Long> grabScript = new DefaultRedisScript<>(
    "local exist = redis.call('HEXISTS', KEYS[1], ARGV[1]) " +
    "if exist == 0 then redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]) return 1 else return 0 end",
    Long.class
);
Long result = redisTemplate.execute(grabScript, Collections.singletonList(orderKey), driverId, System.currentTimeMillis());
```

### 3. RabbitMQ死信队列配置
```java
@Bean
public Queue orderRobQueue() {
    Map<String, Object> args = new HashMap<>(4);
    // 绑定死信交换机与路由key
    args.put("x-dead-letter-exchange", "dlx_exchange");
    args.put("x-dead-letter-routing-key", "order.dlq");
    // 消息10分钟过期
    args.put("x-message-ttl", 10 * 60 * 1000);
    // 队列最大消息容量
    args.put("x-max-length", 2000);
    // 持久化队列
    return new Queue("order_rob_queue", true, false, false, args);
}
```

### 4. Seata分布式事务注解使用
```java
@GlobalTransactional
public void handleOrderPaySuccess(String orderNo) {
    // 更新订单支付状态
    orderInfoFeignClient.updateOrderPayStatus(orderNo);
    // 司机账户分账转账
    driverAccountFeignClient.transfer(transferForm);
}
```

---

## 📊 数据库与端口规划
### 拆分数据库（分库设计）
| 数据库名称 | 业务用途 |
|-----------|---------|
| daijia_customer | 乘客用户信息 |
| daijia_driver | 司机账号、资质、账户 |
| daijia_order | 订单主表、行程记录 |
| daijia_payment | 支付流水、微信支付记录 |
| daijia_coupon | 优惠券、用户券领取记录 |
| daijia_dispatch | 派单调度记录 |
| daijia_map | 简易地理点位缓存 |
| daijia_rules | 计费价格规则 |

### 服务端口对照表
| 服务模块 | 端口 | 说明 |
|---------|------|------|
| server-gateway | 8600 | 统一API网关、文档地址 |
| service-customer | 8501 | 乘客业务服务 |
| service-driver | 8502 | 司机业务服务 |
| service-map | 8503 | 地理位置检索 |
| service-rules | 8504 | 计费规则计算 |
| service-order | 8505 | 订单核心服务 |
| service-payment | 8506 | 支付逻辑 |
| service-dispatch | 8509 | 自动派单调度 |
| service-coupon | 8511 | 优惠券管理 |
| web-customer | 8601 | 乘客小程序接口层 |
| web-driver | 8602 | 司机小程序接口层 |

---

## 🔍 项目优化方向
### ✅ 已完成优化
1. 下单接口CompletableFuture异步解耦，提升QPS、降低RT
2. Redis缓存三大问题完整解决方案（穿透/击穿/雪崩）
3. 循环Feign调用重构批量查询，消除网络IO性能瓶颈
4. Lua+MQ+乐观锁三层并发抢单，压测错误率0%
5. Gateway+Sentinel双层限流熔断，防止微服务雪崩
6. 优惠券预占双回收机制，避免资源丢失
7. Actuator+Prometheus+Grafana全链路监控大盘

### 🔄 待完善优化点
1. 补充单元测试，目标代码覆盖率80%
2. 完善接口JavaDoc注释
3. 接入SonarQube代码质量检测
4. 完善链路追踪SkyWalking
5. 全局统一异常处理细化业务错误码

---

## 📚 项目学习收获
1. **微服务架构落地**：业务垂直拆分、网关路由、远程调用、服务治理完整流程
2. **高并发解决方案**：分布式锁、Lua原子操作、异步削峰、防超卖实战落地
3. **分布式系统难题**：分布式事务、消息可靠性、缓存一致性、定时任务兜底
4. **主流中间件实战**：Redis多种数据结构、RabbitMQ死信队列、MongoDB地理存储、Nacos配置中心
5. **性能与高可用**：接口性能调优、限流熔断、监控告警、故障兜底方案
6. **工程化开发思想**：统一工具封装、分层规范、全局异常、配置解耦