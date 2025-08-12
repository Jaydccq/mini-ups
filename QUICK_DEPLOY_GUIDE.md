# 🚀 Mini-UPS 快速部署指南

本指南帮助你在 5 分钟内设置完成 push 代码后自动部署到 EC2 的完整 CI/CD 流程。

## ⚡ 快速开始

### 1️⃣ 配置 GitHub Secrets (2分钟)

在 GitHub 仓库中添加以下 Secrets：

```bash
# 🔧 基本配置
EC2_HOST=你的EC2公网IP
EC2_USER=ubuntu  
EC2_SSH_KEY=你的完整SSH私钥内容

# 🔐 安全密钥
JWT_SECRET=$(openssl rand -base64 64)
AMAZON_SECRET_KEY=$(openssl rand -base64 32)
```

### 2️⃣ 配置 EC2 安全组 (1分钟)

在 AWS Console 中为你的 EC2 安全组添加入站规则：

```
端口 3000 → 0.0.0.0/0 (UPS Frontend)
端口 8080 → 0.0.0.0/0 (Amazon Service)  
端口 8081 → 0.0.0.0/0 (UPS API)
端口 22   → 你的IP/32 (SSH)
```

### 3️⃣ 初始化 EC2 环境 (2分钟)

SSH 到 EC2 并运行：

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
newgrp docker

# 准备项目目录
mkdir -p /home/ubuntu/mini-ups/{logs,data,backup,images}
sudo mkdir -p /var/log/mini-ups
sudo chown -R ubuntu:ubuntu /var/log/mini-ups
```

## 🎯 自动部署流程

### 触发部署

1. **自动触发** (推荐)：
   ```bash
   git add .
   git commit -m "Deploy to production"
   git push origin main  # ← 自动触发部署
   ```

2. **手动触发**：
   - GitHub → Actions → "Multi-Service Deployment Pipeline" → Run workflow

### 部署过程

```
🧪 运行测试        ← 3-5分钟
🐳 构建镜像        ← 5-8分钟  
📦 传输到EC2       ← 2-3分钟
🚀 启动服务        ← 3-5分钟
🔍 健康检查        ← 2-3分钟
✅ 部署完成        ← 总计15-25分钟
```

### 访问服务

部署完成后访问：

- 🌐 **UPS 前端**: http://你的EC2IP:3000
- 🛒 **Amazon服务**: http://你的EC2IP:8080
- 🔌 **UPS API**: http://你的EC2IP:8081
- 📊 **RabbitMQ管理**: http://你的EC2IP:15672

## 🛠️ 管理命令

SSH 到 EC2 后可用的管理命令：

```bash
cd /home/ubuntu/mini-ups

# 🔍 检查服务健康状态
./scripts/health-check.sh

# 📋 查看日志
./scripts/logs-production.sh
./scripts/logs-production.sh -f ups-backend  # 跟踪特定服务日志

# 🔄 重启服务
./scripts/start-production.sh

# 📊 查看容器状态
docker compose -f docker-compose.production.yml ps

# 📈 监控资源使用
docker stats
```

## 🚨 故障排查

### 常见问题

1. **部署失败：SSH连接被拒绝**
   ```bash
   # 检查安全组是否允许GitHub Actions访问22端口
   # 临时解决：将SSH端口22开放给0.0.0.0/0
   ```

2. **服务无法访问**
   ```bash
   # 检查安全组端口配置
   # 检查服务是否正常启动
   ssh ubuntu@你的EC2IP 'cd mini-ups && docker compose ps'
   ```

3. **部署卡住**
   ```bash
   # 查看GitHub Actions日志
   # SSH到EC2检查资源使用情况
   ssh ubuntu@你的EC2IP 'free -h && df -h'
   ```

### 重置环境

如果需要完全重置：

```bash
# 停止所有服务
cd /home/ubuntu/mini-ups
docker compose -f docker-compose.production.yml down -v

# 清理Docker资源
docker system prune -af
docker volume prune -f

# 重新部署
git push origin main --force-with-lease
```

## 📋 部署检查清单

部署前确认：

- [ ] GitHub Secrets 已配置
- [ ] EC2 安全组已设置
- [ ] EC2 已安装 Docker
- [ ] 项目目录已创建
- [ ] 代码已推送到 main 分支

部署后验证：

- [ ] 所有服务容器正在运行
- [ ] 前端页面可访问 (端口3000)
- [ ] Amazon服务可访问 (端口8080)
- [ ] UPS API健康检查通过 (端口8081/actuator/health)
- [ ] 数据库连接正常

## 🎉 完成！

现在你拥有了一个完全自动化的 Mini-UPS 部署系统：

✅ **Push 即部署**：代码推送后15-25分钟自动部署完成  
✅ **三服务集成**：UPS + Amazon + World Simulator 一键启动  
✅ **健康监控**：自动化健康检查和状态监控  
✅ **日志管理**：集中化日志查看和故障排查  
✅ **生产就绪**：企业级安全配置和最佳实践  

享受你的全自动化开发体验！🚀

---

💡 **提示**: 查看详细配置说明请参考 [`.github/DEPLOYMENT_SETUP.md`](.github/DEPLOYMENT_SETUP.md)