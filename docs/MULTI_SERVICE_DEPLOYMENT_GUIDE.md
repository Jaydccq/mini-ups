# Mini-UPS 多服务部署指南

本指南详细说明如何部署和管理Mini-UPS分布式系统的三个核心服务：UPS物流系统、Amazon电商模拟器和World Simulator环境模拟器。

## 📋 目录

- [系统概述](#系统概述)
- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [本地部署](#本地部署)
- [AWS部署](#aws部署)
- [GitHub Actions CI/CD](#github-actions-cicd)
- [服务管理](#服务管理)
- [故障排除](#故障排除)

## 🎯 系统概述

### 服务架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UPS 服务       │    │   Amazon 服务    │    │ World Simulator │
│                 │    │                 │    │                 │
│ Frontend: 3000  │    │ Web: 8080       │    │ TCP: 12345      │
│ Backend:  8081  │◄──►│ API: REST       │◄──►│ TCP: 23456      │
│ Database: 5431  │    │ Database: 15432 │    │ Database: 5433  │
│ Redis:    6380  │    │                 │    │                 │
│ RabbitMQ: 5672  │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 端口映射

| 服务 | 端口 | 协议 | 用途 |
|------|------|------|------|
| UPS Frontend | 3000 | HTTP | React Web界面 |
| UPS Backend | 8081 | HTTP | Spring Boot API |
| Amazon Web | 8080 | HTTP | Flask Web界面 |
| UPS Database | 5431 | TCP | PostgreSQL |
| Amazon Database | 15432 | TCP | PostgreSQL |
| World Database | 5433 | TCP | PostgreSQL |
| Redis Cache | 6380 | TCP | 缓存服务 |
| RabbitMQ | 5672 | AMQP | 消息队列 |
| RabbitMQ Management | 15672 | HTTP | 管理界面 |
| World Simulator (UPS) | 12345 | TCP | UPS通信 |
| World Simulator (Amazon) | 23456 | TCP | Amazon通信 |

## 📋 前置要求

### 本地开发环境

- **Docker**: 20.10+ 
- **Docker Compose**: 2.0+
- **Git**: 最新版本
- **操作系统**: Linux, macOS, 或 Windows + WSL2

### AWS部署环境

- **AWS EC2实例**: t4g.micro (推荐，Free Tier)
- **操作系统**: Ubuntu 22.04 LTS
- **内存**: 最少1GB RAM
- **存储**: 最少8GB可用空间
- **网络**: 公网IP和安全组配置

### GitHub Actions (可选)

- GitHub仓库权限
- Docker Hub账户 (如果使用镜像仓库)
- AWS EC2访问凭证

## 🚀 快速开始

### 1. 克隆代码

```bash
git clone <repository-url>
cd mini-ups
```

### 2. 配置环境变量

```bash
cp .env.production .env.production.local
# 编辑 .env.production.local，更新密钥和配置
```

### 3. 启动所有服务

```bash
# 使用管理脚本（推荐）
./scripts/start-production.sh

# 或使用Docker Compose
docker-compose -f docker-compose.production.yml --env-file .env.production up -d
```

### 4. 验证部署

```bash
# 运行健康检查
./scripts/health-check.sh

# 查看服务状态
docker-compose -f docker-compose.production.yml ps
```

### 5. 访问服务

- **UPS Frontend**: http://localhost:3000
- **Amazon Service**: http://localhost:8080
- **UPS API**: http://localhost:8081
- **RabbitMQ Management**: http://localhost:15672

## 🏠 本地部署

### 完整本地测试

运行自动化测试脚本验证所有服务：

```bash
./scripts/test-local-deployment.sh
```

此脚本将：
1. ✅ 检查前置条件
2. 🐳 构建Docker镜像
3. 🚀 启动所有服务
4. ⏳ 等待服务就绪
5. 🔍 运行健康检查
6. 🧪 执行集成测试
7. 📊 显示资源使用情况
8. 💪 进行压力测试
9. 🧹 自动清理环境

### 手动部署步骤

#### 1. 构建镜像

```bash
# 构建所有服务镜像
docker-compose -f docker-compose.production.yml build

# 或单独构建
docker-compose -f docker-compose.production.yml build ups-backend
docker-compose -f docker-compose.production.yml build ups-frontend
# Amazon和World Simulator会自动构建
```

#### 2. 启动数据库服务

```bash
# 仅启动数据库
docker-compose -f docker-compose.production.yml up -d ups-database amazon-db world-db ups-redis
```

#### 3. 启动应用服务

```bash
# 启动应用服务
docker-compose -f docker-compose.production.yml up -d ups-backend ups-frontend amazon-web world-server
```

#### 4. 启动消息队列

```bash
# 启动RabbitMQ
docker-compose -f docker-compose.production.yml up -d ups-rabbitmq
```

### 服务配置

#### UPS服务配置

UPS服务使用Spring Boot，主要配置在`application-docker.yml`：

```yaml
spring:
  profiles:
    active: docker
  datasource:
    url: jdbc:postgresql://ups-database:5432/ups_db
    username: postgres
    password: abc123
  redis:
    host: ups-redis
    port: 6379
    password: test123
  rabbitmq:
    host: ups-rabbitmq
    port: 5672
```

#### Amazon服务配置

Amazon服务使用Flask，配置通过环境变量：

```bash
FLASK_ENV=production
DATABASE_URL=postgresql://postgres:abc123@amazon-db:5432/mini_amazon
WORLD_HOST=world-server
WORLD_PORT=23456
UPS_API_URL=http://ups-backend:8081/api
```

#### World Simulator配置

World Simulator使用C++，通过命令行参数配置：

```bash
./server 12345 23456 0
# 12345: UPS连接端口
# 23456: Amazon连接端口
# 0: 其他配置参数
```

## ☁️ AWS部署

### 方法1: 使用部署脚本（推荐）

```bash
./scripts/deploy-aws.sh -h <EC2_IP> -k <SSH_KEY_PATH>
```

脚本参数：
- `-h, --host`: EC2实例IP地址
- `-k, --key`: SSH私钥路径
- `-u, --user`: SSH用户名（默认：ubuntu）
- `-f, --force`: 强制重建镜像
- `-s, --skip-tests`: 跳过本地测试

示例：
```bash
# 基本部署
./scripts/deploy-aws.sh -h 54.123.45.67 -k ~/.ssh/aws-key.pem

# 强制重建并跳过测试
./scripts/deploy-aws.sh -h 54.123.45.67 -k ~/.ssh/aws-key.pem --force --skip-tests
```

### 方法2: 手动部署

#### 1. 准备EC2实例

```bash
# 连接到EC2
ssh -i your-key.pem ubuntu@your-ec2-ip

# 安装Docker
sudo apt update
sudo apt install -y docker.io docker-compose
sudo usermod -aG docker ubuntu
sudo systemctl enable --now docker

# 重新登录以应用组权限
exit
ssh -i your-key.pem ubuntu@your-ec2-ip
```

#### 2. 部署代码

```bash
# 克隆代码
git clone <repository-url>
cd mini-ups

# 配置生产环境
cp .env.production .env.production.aws
# 编辑环境变量，设置生产密钥
```

#### 3. 启动服务

```bash
# 启动所有服务
./scripts/start-production.sh

# 或使用Docker Compose
docker-compose -f docker-compose.production.yml --env-file .env.production.aws up -d
```

#### 4. 配置安全组

在AWS控制台配置安全组，开放以下端口：
- 22 (SSH)
- 3000 (UPS Frontend)
- 8080 (Amazon Service)
- 8081 (UPS API)
- 15672 (RabbitMQ Management, 可选)

### AWS成本优化

推荐配置（月成本 < $5）：

| 资源 | 配置 | 月成本 |
|------|------|--------|
| EC2 t4g.micro | 1 vCPU, 1GB RAM | $0 (Free Tier) |
| EBS Storage | 8GB gp3 | ~$0.80 |
| Elastic IP | 固定公网IP | ~$3.65 |
| 数据传输 | < 1GB/月 | $0 |
| **总计** |  | **< $5/月** |

## 🔄 GitHub Actions CI/CD

### 工作流配置

项目包含两个主要工作流：

1. **`ci-cd.yml`**: UPS服务CI/CD（现有）
2. **`multi-service-deploy.yml`**: 多服务统一部署（新增）

### 使用多服务部署工作流

#### 1. 配置Secrets

在GitHub仓库设置中添加以下Secrets：

```
# AWS部署
AWS_EC2_HOST=your-ec2-public-ip
AWS_EC2_USER=ubuntu
AWS_EC2_SSH_KEY=-----BEGIN RSA PRIVATE KEY-----...

# 应用密钥
JWT_SECRET=your-super-secret-jwt-key-min-256-bits
AMAZON_SECRET_KEY=your-amazon-flask-secret-key

# Docker Hub (可选)
DOCKER_USERNAME=your-docker-username
DOCKER_PASSWORD=your-docker-password
```

#### 2. 触发部署

```bash
# 推送到main分支自动部署
git push origin main

# 手动触发部署
# 在GitHub Actions页面选择 "Multi-Service Deployment Pipeline"
# 点击 "Run workflow" 并选择参数
```

#### 3. 工作流步骤

1. **测试阶段**：
   - UPS后端测试（Java 17 + Maven）
   - UPS前端测试（Node.js + Vitest）
   - 外部服务验证

2. **构建阶段**：
   - 构建所有Docker镜像
   - 缓存优化

3. **集成测试**：
   - 启动完整服务栈
   - 端到端测试

4. **部署阶段**：
   - 部署到AWS EC2
   - 健康检查验证

#### 4. 监控部署

```bash
# 查看部署日志
gh run list --workflow=multi-service-deploy.yml
gh run view <run-id> --log

# 检查部署状态
ssh -i your-key.pem ubuntu@your-ec2-ip './scripts/health-check.sh'
```

## 🔧 服务管理

### 启动服务

```bash
# 启动所有服务（推荐）
./scripts/start-production.sh

# 启动特定服务
docker-compose -f docker-compose.production.yml up -d ups-backend
```

### 查看日志

```bash
# 查看所有服务日志
./scripts/logs-production.sh

# 查看特定服务日志
./scripts/logs-production.sh ups-backend

# 实时跟踪日志
./scripts/logs-production.sh -f

# 查看最近N行日志
./scripts/logs-production.sh -n 50 ups-backend
```

### 健康检查

```bash
# 基本健康检查
./scripts/health-check.sh

# 详细健康检查
./scripts/health-check.sh -d

# 持续监控（每30秒）
./scripts/health-check.sh -c

# JSON格式输出（用于自动化）
./scripts/health-check.sh -j
```

### 备份数据

```bash
# 完整备份
./scripts/backup-production.sh

# 仅备份数据库
./scripts/backup-production.sh -t databases

# 自定义备份目录和保留期
./scripts/backup-production.sh -d /home/ubuntu/backups -r 14
```

### 停止服务

```bash
# 停止所有服务
docker-compose -f docker-compose.production.yml down

# 停止并删除数据卷
docker-compose -f docker-compose.production.yml down -v

# 停止特定服务
docker-compose -f docker-compose.production.yml stop ups-backend
```

### 重启服务

```bash
# 重启所有服务
docker-compose -f docker-compose.production.yml restart

# 重启特定服务
docker-compose -f docker-compose.production.yml restart ups-backend
```

### 更新服务

```bash
# 更新代码
git pull origin main

# 重新构建并重启
docker-compose -f docker-compose.production.yml build
docker-compose -f docker-compose.production.yml up -d
```

## 🔍 故障排除

### 常见问题

#### 1. 服务启动失败

**症状**: 容器无法启动或立即退出

**排查步骤**:
```bash
# 查看容器状态
docker-compose -f docker-compose.production.yml ps

# 查看详细日志
docker-compose -f docker-compose.production.yml logs <service-name>

# 检查镜像是否存在
docker images | grep mini-ups
```

**常见原因**:
- 端口冲突：检查端口是否被占用
- 镜像构建失败：重新构建镜像
- 环境变量配置错误：检查.env文件

#### 2. 数据库连接问题

**症状**: 应用无法连接到数据库

**排查步骤**:
```bash
# 检查数据库容器状态
docker exec mini-ups-postgres pg_isready -U postgres

# 测试数据库连接
docker exec mini-ups-postgres psql -U postgres -d ups_db -c "SELECT 1;"

# 查看数据库日志
docker logs mini-ups-postgres
```

**解决方案**:
- 确保数据库容器正在运行
- 检查数据库密码配置
- 验证网络连接

#### 3. 网络通信问题

**症状**: 服务间无法通信

**排查步骤**:
```bash
# 检查网络配置
docker network ls
docker network inspect mini-ups-network

# 测试容器间连接
docker exec mini-ups-backend ping amazon-web
docker exec mini-ups-backend curl http://amazon-web:8080
```

**解决方案**:
- 确保所有服务在同一网络中
- 检查防火墙设置
- 验证服务名称解析

#### 4. 内存不足

**症状**: 服务性能差或OOM错误

**排查步骤**:
```bash
# 检查系统资源
free -h
df -h

# 检查容器资源使用
docker stats
```

**解决方案**:
- 减少并发连接数
- 优化JVM内存设置
- 考虑升级实例规格

#### 5. World Simulator TCP连接问题

**症状**: UPS或Amazon无法连接到World Simulator

**排查步骤**:
```bash
# 检查端口监听
netstat -ln | grep -E "(12345|23456)"

# 检查World Simulator日志
docker logs world-simulator-server

# 测试TCP连接
telnet localhost 12345
telnet localhost 23456
```

**解决方案**:
- 确保World Simulator容器运行正常
- 检查端口映射配置
- 验证防火墙规则

### 日志分析

#### 应用日志位置

- **UPS后端**: `/var/log/mini-ups/mini-ups.log`
- **容器日志**: `docker logs <container-name>`
- **系统日志**: `/var/log/syslog`

#### 常见错误模式

```bash
# 查找错误
./scripts/logs-production.sh | grep -i error

# 查找连接问题
./scripts/logs-production.sh | grep -i "connection"

# 查找内存问题
./scripts/logs-production.sh | grep -i "memory\|oom"
```

### 性能优化

#### 1. 数据库优化

```sql
-- 连接到数据库
docker exec -it mini-ups-postgres psql -U postgres -d ups_db

-- 查看慢查询
SELECT query, calls, total_time, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;

-- 查看数据库大小
SELECT pg_size_pretty(pg_database_size('ups_db'));
```

#### 2. 内存优化

```bash
# 设置JVM内存限制
export JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 设置Docker内存限制
docker-compose -f docker-compose.production.yml up -d --memory=1g ups-backend
```

#### 3. 网络优化

```bash
# 检查网络延迟
docker exec mini-ups-backend ping -c 5 amazon-web

# 优化连接池配置
# 在application.yml中设置
spring.datasource.hikari.maximum-pool-size=10
```

### 监控和告警

#### 1. 基础监控

```bash
# 持续健康检查
./scripts/health-check.sh -c

# 资源监控
watch -n 5 'docker stats --no-stream'

# 磁盘空间监控
watch -n 60 'df -h'
```

#### 2. 自动告警脚本

```bash
#!/bin/bash
# 创建 monitor.sh
while true; do
    if ! ./scripts/health-check.sh > /dev/null; then
        echo "$(date): Health check failed" >> /var/log/mini-ups/alerts.log
        # 发送告警邮件或通知
    fi
    sleep 300  # 5分钟检查一次
done
```

### 数据恢复

#### 1. 从备份恢复数据库

```bash
# UPS数据库恢复
docker exec -i mini-ups-postgres psql -U postgres -d ups_db < backup/ups_database.sql

# Amazon数据库恢复
docker exec -i mini-amazon-db psql -U postgres -d mini_amazon < backup/amazon_database.sql

# World数据库恢复
docker exec -i world-simulator-db psql -U postgres -d worldSim < backup/world_database.sql
```

#### 2. Redis数据恢复

```bash
# 停止Redis
docker-compose -f docker-compose.production.yml stop ups-redis

# 恢复数据文件
docker cp backup/redis_dump.rdb mini-ups-redis:/data/dump.rdb

# 启动Redis
docker-compose -f docker-compose.production.yml start ups-redis
```

## 📚 附录

### 环境变量参考

完整的环境变量配置：

```bash
# UPS服务配置
UPS_POSTGRES_DB=ups_db
UPS_POSTGRES_USER=postgres
UPS_POSTGRES_PASSWORD=abc123
UPS_REDIS_PASSWORD=test123
UPS_RABBITMQ_USER=guest
UPS_RABBITMQ_PASSWORD=guest
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-min-256-bits
NUM_TRUCKS=5
WORLD_ID=1

# Amazon服务配置
AMAZON_SECRET_KEY=your-amazon-flask-secret-key-change-this-in-production

# Docker镜像配置
UPS_BACKEND_IMAGE=mini-ups-backend:latest
UPS_FRONTEND_IMAGE=mini-ups-frontend:latest

# 日志和监控
LOG_LEVEL=INFO
LOG_PATH=/var/log/mini-ups
BACKUP_RETENTION_DAYS=7
BACKUP_PATH=/var/backups/mini-ups

# 性能调优
UPS_DB_POOL_SIZE=10
AMAZON_DB_POOL_SIZE=5
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

### 端口和网络配置

| 服务 | 容器名称 | 内部端口 | 外部端口 | 网络 |
|------|----------|----------|----------|------|
| UPS Backend | mini-ups-backend | 8081 | 8081 | mini-ups-network |
| UPS Frontend | mini-ups-frontend | 80 | 3000 | mini-ups-network |
| UPS Database | mini-ups-postgres | 5432 | 5431 | mini-ups-network |
| UPS Redis | mini-ups-redis | 6379 | 6380 | mini-ups-network |
| UPS RabbitMQ | mini-ups-rabbitmq | 5672/15672 | 5672/15672 | mini-ups-network |
| Amazon Web | mini-amazon-web | 8080 | 8080 | mini-ups-network |
| Amazon Database | mini-amazon-db | 5432 | 15432 | mini-ups-network |
| World Server | world-simulator-server | 12345/23456 | 12345/23456 | mini-ups-network |
| World Database | world-simulator-db | 5432 | 5433 | mini-ups-network |

### 有用的命令

```bash
# 快速状态检查
docker-compose -f docker-compose.production.yml ps

# 查看所有容器资源使用
docker stats

# 清理未使用的Docker资源
docker system prune -af

# 查看Docker磁盘使用
docker system df

# 备份整个Docker环境
docker save $(docker images -q) -o docker-images-backup.tar

# 查看容器详细信息
docker inspect mini-ups-backend | jq '.[0].Config'

# 进入容器调试
docker exec -it mini-ups-backend bash

# 查看网络配置
docker network inspect mini-ups-network
```

## 🆘 获取帮助

如果遇到问题：

1. **查看日志**: `./scripts/logs-production.sh -f`
2. **运行健康检查**: `./scripts/health-check.sh -d`
3. **检查GitHub Issues**: 搜索相似问题
4. **查看文档**: 参考项目README和CLAUDE.md
5. **联系支持**: 提交Issue或联系维护团队

---

📝 **文档版本**: v1.0.0  
📅 **最后更新**: 2024年  
👥 **维护**: Mini-UPS开发团队