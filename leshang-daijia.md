## 核心技术

- **SpringBoot**：简化Spring应用的初始搭建以及开发过程
- **SpringCloud**：基于Spring Boot实现的云原生应用开发工具，SpringCloud使用的技术：（Spring Cloud Gateway、Spring Cloud Task和Spring Cloud Feign等）
- **SpringBoot+SpringCloudAlibaba(Nacos，Sentinel) + OpenFeign + Gateway**
- MyBatis-Plus：持久层框架，也依赖mybatis
- Redis：内存做缓存
- Redisson：基于redis的Java驻内存数据网格 - 框架；操作redis的框架
- MongoDB: 分布式文件存储的数据库
- RabbitMQ：消息中间件；大型分布式项目是标配；分布式事务最终一致性
- Seata：分布式事务
- Drools：规则引擎，计算预估费用、取消费用等等
- GEO：GPS分区定位计算
- ThreadPoolExecutor+CompletableFuture：异步编排，线程池来实现异步操作，提高效率
- XXL-JOB: 分布式定时任务调用中心
- Knife4J/YAPI：Api接口文档工具
- MinIO（私有化对象存储集群）：分布式文件存储 类似于OSS（公有）
- 微信支付：微信支付与微信分账
- MySQL：关系型数据库 {shardingSphere-jdbc 进行读写分离; 分库，分表}
- Lombok: 实体类的中get/set 生成的jar包
- Natapp：内网穿透
- Docker：容器化技术;  生产环境Redis（运维人员）；快速搭建环境Docker run
- Git：代码管理工具；Git使用，拉代码、提交、推送、合并、冲突解决


## 从github上重新拉取项目
克隆时直接指定分支（最常用）
使用 -b 或 --branch 参数，后面跟上分支名。
```bash
git clone -b 分支名 https://github.com/你的用户名/你的仓库名.git
```