# 乐尚代驾 - Docker 中间件部署指南

## 🚀 一键启动（最简单）
powershell
# 1. 确保 Docker Desktop 正在运行
docker info

# 2. 进入项目目录
cd E:\codebase\leshang-daijia-20260104\docker-middleware

# 3. 启动所有中间件
docker-compose up -d

# 4. 等待 30 秒让服务初始化
Start-Sleep -Seconds 30

# 5. 查看运行状态
docker ps
```
## 📊 中间件访问地址

| 服务 | 地址 | 账号/密码 |
|------|------|----------|
| MySQL | localhost:3306 | root / root123456 |
| Redis | localhost:6379 | 无密码 |
| MongoDB | localhost:27017 | 无密码 |
| RabbitMQ | http://localhost:15672 | admin / admin123 |
| Nacos | http://localhost:8848/nacos | nacos / nacos |
| XXL-Job | http://localhost:8080/xxl-job-admin | admin / 123456 |
| Seata | localhost:8091 | 无认证 |

## 🔧 常用命令

```
powershell
# 查看日志
docker-compose logs -f [服务名]  # 如: docker-compose logs -f mysql

# 停止所有服务
docker-compose down

# 重启某个服务
docker-compose restart mysql

# 清理数据卷（慎用！会删除所有数据）
docker-compose down -v
```
## ⚠️ 注意事项

1. **首次启动需要时间**：MySQL 初始化数据库约需 1-2 分钟
2. **端口冲突**：确保 3306、6379、8848 等端口未被占用
3. **数据持久化**：数据保存在 Docker Volume 中，删除容器不会丢失数据

## 🛠️ 故障排查

### MySQL 启动失败
```
powershell
# 查看日志
docker logs daijia-mysql

# 检查端口占用
netstat -ano | findstr :3306
```
### Nacos 启动失败
```
powershell
# 查看日志
docker logs daijia-nacos

# 确认 MySQL 已就绪
docker exec -it daijia-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"
```
## 📦 包含的中间件

- ✅ MySQL 8.0.33（业务数据库）
- ✅ Redis 7（缓存/分布式锁）
- ✅ MongoDB 5（司机轨迹存储）
- ✅ RabbitMQ 3-management（消息队列）
- ✅ Nacos 2.3.2（注册中心/配置中心）
- ✅ XXL-Job 2.4.0（定时任务）
- ✅ Seata 1.7.0（分布式事务）

## 🎉 完成标志

当 `docker ps` 看到所有容器状态为 `Up` 时，部署成功！
```
---

## 💡 **总结**

**docker-middleware 的作用就是**：用一条命令启动所有中间件，不用你在本地安装 MySQL、Redis 等软件。

**为什么有这么多文件？**
- `docker-compose.yml`：定义所有中间件的配置
- `init-sql/*.sql`：自动初始化数据库（建库、建表）

**对！你理解得很准确！** 👍

## 📦 **Docker 管理中间件的核心流程**

### **1. 编写 docker-compose.yml（定义中间件）**
```yaml
services:
  mysql:           # 定义 MySQL 服务
    image: mysql:8.0.33
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123456
```


### **2. 准备初始化脚本（可选）**
- SQL 文件放在 `init-sql/` 目录
- 容器首次启动时会自动执行这些脚本

### **3. 启动容器**
```powershell
docker-compose up -d
```

就这么简单！✅

**用 Docker 管理中间件的优势：**
1. ✅ **不用在本地安装** MySQL、Redis 等软件
2. ✅ **一键启动**所有中间件（一条命令）
3. ✅ **环境隔离**（不影响其他项目）
4. ✅ **数据持久化**（删除容器数据不丢失）
5. ✅ **易于迁移**（换台机器也能快速部署）
