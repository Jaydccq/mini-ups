# EC2 完整设置指南

## 🎯 目标
在 EC2 上设置 Mini-UPS，支持从 GitHub Container Registry 拉取镜像

## 📋 步骤1：连接 EC2 并安装基础环境

### 连接到 EC2
```bash
# 在本地执行
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190
```

### 安装 Docker 和 Docker Compose
```bash
# 更新系统 (Amazon Linux 2023)
sudo dnf update -y

# 安装 Docker
sudo dnf install -y docker git
sudo systemctl start docker
sudo systemctl enable docker

# 将用户添加到 docker 组
sudo usermod -a -G docker ec2-user

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 退出并重新登录以应用组权限
exit
```

### 重新连接并验证
```bash
# 重新连接
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190

# 验证安装
docker --version
docker-compose --version
docker info
```

## 📋 步骤2：设置项目目录

```bash
# 创建项目目录
mkdir -p /home/ec2-user/mini-ups
cd /home/ec2-user/mini-ups

# 克隆代码库 (包含 Amazon 和 World Simulator)
git clone https://github.com/你的用户名/mini-ups.git .

# 创建必要的网络
docker network create projectnet || true
```

## 📋 步骤3：配置 GitHub Container Registry 访问

### 生成 GitHub Personal Access Token
1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token (classic)"
3. 选择 scopes:
   - `read:packages` (读取容器镜像)
   - `repo` (如果仓库是私有的)
4. 复制生成的 token

### 在 EC2 上登录 GitHub Container Registry
```bash
# 使用你的 GitHub 用户名和刚生成的 token
echo "你的_GITHUB_TOKEN" | docker login ghcr.io -u Jaydccq --password-stdin

# 验证登录
docker info | grep -A5 "Registry Mirrors"
```

## 📋 步骤4：创建环境配置文件

### 创建 .env 文件
```bash
cat > .env << 'EOF'
# Database Configuration
POSTGRES_DB=ups_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=你的数据库密码

# Redis Configuration
REDIS_PASSWORD=你的Redis密码

# Application Configuration
JWT_SECRET=你的JWT密钥_至少32字符长度
SPRING_PROFILES_ACTIVE=production

# Docker Images (由 CI/CD 更新)
BACKEND_IMAGE=ghcr.io/Jaydccq/mini-ups/backend:latest
FRONTEND_IMAGE=ghcr.io/Jaydccq/mini-ups/frontend:latest

# External Access
PUBLIC_IP=44.219.181.190
VITE_API_BASE_URL=http://44.219.181.190:8081/api
VITE_WS_BASE_URL=ws://44.219.181.190:8081/ws

# Memory Optimization for t2.micro
JAVA_OPTS=-Xmx300m -Xms150m -XX:+UseG1GC

# Feature Flags (disable resource-intensive features)
WORLD_SIMULATOR_ENABLED=false
RABBITMQ_ENABLED=false
NOTIFICATIONS_ENABLED=false
EOF
```

## 📋 步骤5：理解镜像拉取流程

### 本地构建 vs 远程拉取

**本地构建模式 (start-all.sh):**
```bash
# docker-compose.yml 中的配置
services:
  ups-backend:
    image: mini-ups-backend:latest  # 本地镜像名
    build:                          # 本地构建配置
      context: ./backend
      dockerfile: Dockerfile
```

**远程拉取模式 (CI/CD):**
```bash
# 环境变量覆盖镜像名
BACKEND_IMAGE=ghcr.io/你的用户名/mini-ups/backend:sha-abc123

# docker-compose.yml 使用环境变量
services:
  ups-backend:
    image: ${BACKEND_IMAGE:-mini-ups-backend:latest}  # 优先使用环境变量
    # build 部分被忽略，直接拉取镜像
```

### 镜像拉取命令示例
```bash
# 手动拉取特定版本的镜像
docker pull ghcr.io/你的用户名/mini-ups/backend:sha-abc123
docker pull ghcr.io/你的用户名/mini-ups/frontend:sha-abc123

# 或拉取最新版本
docker pull ghcr.io/Jaydccq/mini-ups/backend:latest
docker pull ghcr.io/Jaydccq/mini-ups/frontend:latest

# 查看本地镜像
docker images | grep mini-ups
```

## 📋 步骤6：部署选择

### 选项A：完整系统部署 (需要更多内存)
```bash
# 使用原版 docker-compose.yml (包含 Amazon + World Simulator)
docker compose up -d
```

### 选项B：精简部署 (推荐用于 t2.micro)
```bash
# 使用优化版本 (仅 UPS 服务)
docker compose -f docker-compose.t2micro.yml up -d
```

## 📋 步骤7：验证部署

```bash
# 检查容器状态
docker compose ps

# 查看日志
docker compose logs -f ups-backend
docker compose logs -f ups-frontend

# 健康检查
curl http://localhost:8081/actuator/health
curl http://localhost:3000

# 检查资源使用
docker stats --no-stream
```

## 🔧 CI/CD 自动部署流程

### GitHub Actions 做什么：
1. **构建阶段**：
   ```yaml
   # 构建 Docker 镜像
   docker build -t ghcr.io/用户名/mini-ups/backend:sha-abc123 ./backend
   docker build -t ghcr.io/用户名/mini-ups/frontend:sha-abc123 ./frontend
   
   # 推送到 GitHub Container Registry
   docker push ghcr.io/用户名/mini-ups/backend:sha-abc123
   docker push ghcr.io/用户名/mini-ups/frontend:sha-abc123
   ```

2. **部署阶段**：
   ```bash
   # 在 EC2 上执行
   cd /home/ec2-user/mini-ups
   git pull origin main  # 拉取最新的 docker-compose 配置
   
   # 设置环境变量指向新镜像
   export BACKEND_IMAGE=ghcr.io/用户名/mini-ups/backend:sha-abc123
   export FRONTEND_IMAGE=ghcr.io/用户名/mini-ups/frontend:sha-abc123
   
   # 停止旧服务，拉取新镜像，启动新服务
   docker compose down
   docker compose pull  # 拉取新镜像
   docker compose up -d  # 启动新服务
   ```

## 🚀 触发部署

### 自动触发 (推送到 main 分支)
```bash
# 在本地
git add .
git commit -m "deploy: update mini-ups"
git push origin main
```

### 手动触发 (GitHub Actions)
1. 访问 GitHub repository
2. Actions 标签
3. 选择 "Mini-UPS CI/CD Pipeline"
4. 点击 "Run workflow"
5. 选择环境 (staging/production)

## 🌐 访问应用

部署成功后：
- **前端**: http://44.219.181.190:3000
- **后端 API**: http://44.219.181.190:8081/api
- **健康检查**: http://44.219.181.190:8081/actuator/health

## 🔍 故障排查

### 镜像拉取失败
```bash
# 检查登录状态
docker system info | grep -i registry

# 重新登录
echo "你的_GITHUB_TOKEN" | docker login ghcr.io -u Jaydccq --password-stdin

# 手动拉取测试
docker pull ghcr.io/Jaydccq/mini-ups/backend:latest
```

### 内存不足
```bash
# 检查内存使用
free -h
docker stats --no-stream

# 清理不用的镜像和容器
docker system prune -f
docker image prune -f
```

### 服务启动失败
```bash
# 查看详细日志
docker compose logs ups-backend
docker compose logs ups-frontend

# 检查端口占用
sudo netstat -tlnp | grep :8081
sudo netstat -tlnp | grep :3000
```