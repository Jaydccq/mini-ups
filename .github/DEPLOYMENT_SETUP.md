# Mini-UPS 自动化部署设置指南

本文档详细说明如何配置 EC2 环境和 GitHub Actions，实现 push 代码后自动部署的完整 CI/CD 流程。

## 🎯 目标架构

```
┌─────────────┐    ┌──────────────┐    ┌─────────────────┐
│ GitHub Repo │───▶│ GitHub       │───▶│ AWS EC2         │
│             │    │ Actions      │    │                 │
│ Push Code   │    │ CI/CD        │    │ ┌─────────────┐ │
└─────────────┘    │              │    │ │ UPS Service │ │
                   │ Build Images │    │ │ (Port 3000) │ │
                   │ Run Tests    │    │ └─────────────┘ │
                   │ Deploy       │    │ ┌─────────────┐ │
                   └──────────────┘    │ │Amazon Service│ │
                                       │ │ (Port 8080) │ │
                                       │ └─────────────┘ │
                                       │ ┌─────────────┐ │
                                       │ │World Sim    │ │
                                       │ │(Port 12345) │ │
                                       │ └─────────────┘ │
                                       └─────────────────┘
```

## 📋 前置条件

1. ✅ AWS EC2 实例运行中
2. ✅ GitHub 仓库已设置
3. ✅ Docker 和 Docker Compose 已安装在 EC2
4. ⚠️ 需要配置安全组和 GitHub Secrets

## 🔐 第一步：配置 EC2 安全组

### 1.1 登录 AWS Console

1. 访问 [AWS Console](https://console.aws.amazon.com/)
2. 导航到 **EC2 服务**
3. 在左侧导航栏选择 **Security Groups**

### 1.2 配置入站规则

找到你的 EC2 实例对应的安全组，添加以下入站规则：

| 类型 | 协议 | 端口范围 | 源 | 描述 |
|------|------|----------|-----|------|
| HTTP | TCP | 80 | 0.0.0.0/0 | Web 访问 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS 访问 |
| Custom TCP | TCP | 3000 | 0.0.0.0/0 | UPS Frontend |
| Custom TCP | TCP | 8080 | 0.0.0.0/0 | Amazon Service |
| Custom TCP | TCP | 8081 | 0.0.0.0/0 | UPS Backend API |
| Custom TCP | TCP | 15672 | 你的IP/32 | RabbitMQ Management |
| SSH | TCP | 22 | 你的IP/32 | SSH 访问 |

**⚠️ 安全建议:**
- RabbitMQ Management (15672) 仅允许你的 IP 访问
- SSH (22) 仅允许必要的 IP 访问
- 生产环境建议使用 Application Load Balancer

### 1.3 配置出站规则

确保出站规则允许：
- **All traffic** to **0.0.0.0/0** (用于下载 Docker 镜像和系统更新)

## 🔑 第二步：设置 GitHub Secrets

### 2.1 获取必要信息

#### EC2 连接信息：
```bash
# 1. 获取 EC2 公网 IP
aws ec2 describe-instances --instance-ids YOUR_INSTANCE_ID --query 'Reservations[0].Instances[0].PublicIpAddress'

# 2. 准备 SSH 私钥内容
cat ~/.ssh/your-ec2-key.pem
```

#### 生成安全密钥：
```bash
# 生成 JWT Secret (256位)
openssl rand -base64 64

# 生成 Amazon Secret Key
openssl rand -base64 32
```

### 2.2 在 GitHub 中添加 Secrets

1. 进入你的 GitHub 仓库
2. 点击 **Settings** 选项卡
3. 在左侧导航栏选择 **Secrets and variables** → **Actions**
4. 点击 **New repository secret**

添加以下 Secrets：

| Secret Name | 值 | 描述 |
|-------------|-----|------|
| `EC2_HOST` | `你的EC2公网IP` | EC2 实例的公网 IP 地址 |
| `EC2_USER` | `ubuntu` | EC2 SSH 用户名 (Ubuntu 实例通常是 ubuntu) |
| `EC2_SSH_KEY` | `-----BEGIN RSA PRIVATE KEY-----...` | 你的 EC2 SSH 私钥内容 (完整内容) |
| `JWT_SECRET` | `base64编码的密钥` | JWT 签名密钥 (256位) |
| `AMAZON_SECRET_KEY` | `base64编码的密钥` | Amazon 服务密钥 |
| `DOCKER_USERNAME` | `你的DockerHub用户名` | (可选) Docker Hub 用户名 |
| `DOCKER_PASSWORD` | `你的DockerHub密码` | (可选) Docker Hub 密码 |

### 2.3 验证 Secrets 设置

设置完成后，在 Repository secrets 页面应该能看到所有必需的 secrets：

```
✅ EC2_HOST
✅ EC2_USER  
✅ EC2_SSH_KEY
✅ JWT_SECRET
✅ AMAZON_SECRET_KEY
⚪ DOCKER_USERNAME (可选)
⚪ DOCKER_PASSWORD (可选)
```

## 🚀 第三步：初始化 EC2 环境

### 3.1 SSH 连接到 EC2

```bash
ssh -i your-key.pem ubuntu@YOUR_EC2_IP
```

### 3.2 安装必要依赖

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu
newgrp docker

# 安装 Docker Compose
sudo apt install docker-compose-plugin

# 安装其他工具
sudo apt install git curl wget htop net-tools -y

# 验证安装
docker --version
docker compose version
```

### 3.3 准备项目目录

```bash
# 创建项目目录
mkdir -p /home/ubuntu/mini-ups
cd /home/ubuntu/mini-ups

# 创建必要的目录
mkdir -p logs data backup images

# 设置权限
sudo mkdir -p /var/log/mini-ups /var/backups/mini-ups
sudo chown -R ubuntu:ubuntu /var/log/mini-ups /var/backups/mini-ups
```

### 3.4 测试 Docker 网络

```bash
# 创建项目网络
docker network create mini-ups-network

# 验证网络
docker network ls
```

## 🔧 第四步：配置自动部署

### 4.1 创建 systemd 服务 (可选)

为了确保服务在系统重启后自动启动：

```bash
sudo tee /etc/systemd/system/mini-ups.service > /dev/null <<EOF
[Unit]
Description=Mini-UPS Application
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ubuntu/mini-ups
ExecStart=/home/ubuntu/mini-ups/scripts/start-production.sh
ExecStop=/usr/bin/docker-compose -f docker-compose.production.yml down
User=ubuntu
Group=ubuntu

[Install]
WantedBy=multi-user.target
EOF

# 启用服务
sudo systemctl enable mini-ups.service
```

### 4.2 设置日志轮转

```bash
sudo tee /etc/logrotate.d/mini-ups > /dev/null <<EOF
/var/log/mini-ups/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 644 ubuntu ubuntu
    postrotate
        /usr/bin/docker exec mini-ups-backend kill -USR1 1 2>/dev/null || true
    endscript
}
EOF
```

## 🧪 第五步：测试部署流程

### 5.1 手动触发部署

1. 在 GitHub 仓库中，进入 **Actions** 选项卡
2. 选择 **Multi-Service Deployment Pipeline**
3. 点击 **Run workflow**
4. 选择 **main** 分支
5. 设置 **Deploy to AWS EC2** 为 **true**
6. 点击 **Run workflow**

### 5.2 监控部署过程

在 Actions 页面监控部署进度：

```
🧪 Test UPS Services          ← 运行单元测试和集成测试
🐳 Build Docker Images        ← 构建 Docker 镜像
🔍 Integration Test           ← 运行集成测试
🚀 Deploy to AWS EC2          ← 部署到 EC2
📨 Notify Deployment Result   ← 发送部署结果通知
```

### 5.3 验证部署结果

部署完成后，访问以下 URL 验证服务：

```bash
# 检查服务状态
ssh -i your-key.pem ubuntu@YOUR_EC2_IP 'cd mini-ups && docker compose -f docker-compose.production.yml ps'

# 访问服务
curl http://YOUR_EC2_IP:3000    # UPS Frontend
curl http://YOUR_EC2_IP:8080    # Amazon Service  
curl http://YOUR_EC2_IP:8081/actuator/health  # UPS Backend Health
```

## 🔄 第六步：自动化部署流程

### 6.1 自动部署触发条件

部署将在以下情况自动触发：

1. **Push 到 main 分支**：
   ```bash
   git checkout main
   git add .
   git commit -m "Your changes"
   git push origin main
   ```

2. **Pull Request 合并到 main**：
   - 创建 Pull Request
   - 代码审查通过
   - 合并 PR → 自动触发部署

### 6.2 部署流程时间线

典型的部署流程耗时：

```
📝 代码推送                    ← 0分钟
🧪 运行测试                    ← 3-5分钟  
🐳 构建镜像                    ← 5-8分钟
📦 上传到 EC2                  ← 2-3分钟
🚀 部署服务                    ← 3-5分钟
🔍 健康检查                    ← 2-3分钟
✅ 部署完成                    ← 总计：15-25分钟
```

## 📊 第七步：监控和维护

### 7.1 监控脚本

```bash
# 在 EC2 上设置监控
crontab -e

# 添加以下内容：
# 每5分钟检查服务健康状态
*/5 * * * * /home/ubuntu/mini-ups/scripts/health-check.sh > /dev/null 2>&1

# 每天备份数据
0 2 * * * /home/ubuntu/mini-ups/scripts/backup-production.sh > /dev/null 2>&1
```

### 7.2 常用管理命令

```bash
# SSH 到 EC2
ssh -i your-key.pem ubuntu@YOUR_EC2_IP

# 查看服务状态
cd mini-ups && ./scripts/health-check.sh

# 查看日志
cd mini-ups && ./scripts/logs-production.sh

# 重启服务
cd mini-ups && ./scripts/start-production.sh

# 查看资源使用
docker stats

# 清理空间
docker system prune -f
```

## 🚨 故障排查

### 常见问题和解决方案

#### 1. 部署失败：SSH 连接超时
```bash
# 检查安全组设置
# 确保 22 端口对 GitHub Actions IP 开放

# 或者使用 GitHub Actions 的 IP 范围
# 在安全组中添加 GitHub Actions 的 IP 范围
```

#### 2. 服务启动失败：端口冲突
```bash
# 检查端口占用
sudo netstat -tulpn | grep -E "(3000|8080|8081)"

# 杀死占用进程
sudo fuser -k 3000/tcp
```

#### 3. Docker 镜像拉取失败
```bash
# 检查网络连接
curl -I https://registry-1.docker.io/

# 重启 Docker 服务
sudo systemctl restart docker
```

#### 4. 数据库连接失败
```bash
# 检查数据库容器状态
docker ps | grep postgres

# 查看数据库日志
docker logs mini-ups-postgres
```

## 🎉 完成！

设置完成后，你的 Mini-UPS 系统将具备：

✅ **自动化 CI/CD**：Push 代码即自动部署  
✅ **全服务部署**：UPS + Amazon + World Simulator  
✅ **健康监控**：自动健康检查和告警  
✅ **日志管理**：集中化日志查看和分析  
✅ **安全配置**：最小权限原则的安全组设置  
✅ **备份恢复**：自动化数据备份  

### 快速访问链接

部署完成后，你可以通过以下链接访问服务：

- 🌐 **UPS Frontend**: http://YOUR_EC2_IP:3000
- 🛒 **Amazon Service**: http://YOUR_EC2_IP:8080  
- 🔌 **UPS Backend API**: http://YOUR_EC2_IP:8081
- 📊 **RabbitMQ Management**: http://YOUR_EC2_IP:15672

享受你的全自动化 Mini-UPS 部署系统！ 🚀