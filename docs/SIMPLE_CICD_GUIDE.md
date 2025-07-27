# 🚀 Mini-UPS 简化版 GitHub Actions + AWS 部署指南

专为个人项目设计的轻量级CI/CD和AWS部署方案，成本低廉且易于维护。

## 📋 概览

**架构特点：**
- ✅ 单个EC2实例运行所有服务
- ✅ Docker Compose统一管理
- ✅ 简单的GitHub Actions工作流
- ✅ 成本控制在$6/月以内

```
GitHub Repository
       ↓ (push to main)
GitHub Actions
       ↓ (SSH deploy)
EC2 Instance (t3.micro)
       ↓ (docker compose)
┌─────────────────────────┐
│  Nginx (Frontend)       │
│  Spring Boot (Backend)  │
│  PostgreSQL (Database)  │
│  Redis (Cache)          │
└─────────────────────────┘
```

---

## 🛠️ AWS设置 (一次性配置)

### 步骤1: 创建EC2实例

```bash
# 1. 创建密钥对
aws ec2 create-key-pair \
  --key-name mini-ups-key \
  --query 'KeyMaterial' \
  --output text > mini-ups-key.pem

chmod 400 mini-ups-key.pem

# 2. 创建安全组
aws ec2 create-security-group \
  --group-name mini-ups-sg \
  --description "Mini-UPS Security Group"

# 获取安全组ID
SECURITY_GROUP_ID=$(aws ec2 describe-security-groups \
  --group-names mini-ups-sg \
  --query 'SecurityGroups[0].GroupId' \
  --output text)

# 3. 配置安全组规则
# ⚠️ 安全提醒：请将YOUR_IP替换为你的实际IP地址，不要使用0.0.0.0/0
aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp --port 22 --cidr YOUR_IP/32 # SSH (仅限你的IP)

aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp --port 80 --cidr 0.0.0.0/0 # HTTP

aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp --port 443 --cidr 0.0.0.0/0 # HTTPS

# 4. 启动EC2实例
aws ec2 run-instances \
  --image-id ami-0c02fb55956c7d316 \
  --count 1 \
  --instance-type t3.micro \
  --key-name mini-ups-key \
  --security-group-ids $SECURITY_GROUP_ID \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=mini-ups-server}]'
```

### 步骤2: 配置服务器环境

```bash
# SSH连接到服务器
ssh -i mini-ups-key.pem ec2-user@YOUR_EC2_IP

# 安装Docker和Docker Compose
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 安装Git
sudo yum install -y git

# 重新登录以获取Docker权限
exit
ssh -i mini-ups-key.pem ec2-user@YOUR_EC2_IP
```

---

## 🔐 GitHub Secrets配置

在GitHub仓库中添加以下Secrets (Settings > Secrets and variables > Actions):

```bash
# 服务器连接
EC2_HOST: YOUR_EC2_PUBLIC_IP
EC2_USER: ec2-user
EC2_SSH_KEY: |
  -----BEGIN RSA PRIVATE KEY-----
  (复制mini-ups-key.pem内容)
  -----END RSA PRIVATE KEY-----

# 应用配置
DB_PASSWORD: your_secure_password_123
JWT_SECRET: your_super_secret_jwt_key_here
REDIS_PASSWORD: your_redis_password_123

# 可选：外部服务
WORLD_SIMULATOR_HOST: your-world-simulator-host
AMAZON_API_URL: http://your-amazon-service-url
```

---

## 🚀 GitHub Actions工作流

创建文件: `.github/workflows/deploy.yml`

```yaml
name: Deploy to AWS EC2

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: mini_ups_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
    - name: Checkout代码
      uses: actions/checkout@v4

    - name: 设置Java 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: 设置Node.js 18
      uses: actions/setup-node@v4
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json

    # 后端测试
    - name: 后端测试
      run: |
        cd backend
        mvn clean test -Dspring.profiles.active=test
      env:
        POSTGRES_URL: jdbc:postgresql://localhost:5432/mini_ups_test
        POSTGRES_USER: postgres
        POSTGRES_PASSWORD: postgres
        REDIS_HOST: localhost
        REDIS_PORT: 6379

    # 前端测试
    - name: 前端测试
      run: |
        cd frontend
        npm ci
        npm run test
        npm run lint

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
    - name: Checkout代码
      uses: actions/checkout@v4

    - name: 部署到EC2
      uses: appleboy/ssh-action@v1.0.0
      with:
        host: ${{ secrets.EC2_HOST }}
        username: ${{ secrets.EC2_USER }}
        key: ${{ secrets.EC2_SSH_KEY }}
        script: |
          # 进入项目目录
          cd /home/ec2-user/mini-ups || {
            echo "首次部署，克隆仓库..."
            git clone https://github.com/${{ github.repository }}.git mini-ups
            cd mini-ups
          }
          
          # 更新代码
          git fetch origin
          git reset --hard origin/main
          
          # 创建环境变量文件
          cat > .env << EOF
          # 数据库配置
          POSTGRES_DB=ups_db
          POSTGRES_USER=postgres
          POSTGRES_PASSWORD=${{ secrets.DB_PASSWORD }}
          
          # Redis配置
          REDIS_PASSWORD=${{ secrets.REDIS_PASSWORD }}
          
          # 应用配置
          JWT_SECRET=${{ secrets.JWT_SECRET }}
          SPRING_PROFILES_ACTIVE=production
          
          # 外部服务 (可选)
          WORLD_SIMULATOR_HOST=${{ secrets.WORLD_SIMULATOR_HOST }}
          AMAZON_API_URL=${{ secrets.AMAZON_API_URL }}
          EOF
          
          # 停止现有服务
          docker compose down || true
          
          # 构建并启动服务
          docker compose up --build -d
          
          # 等待服务启动
          sleep 30
          
          # 健康检查
          echo "执行健康检查..."
          if curl -f http://localhost:8081/api/actuator/health; then
            echo "✅ 后端健康检查通过"
          else
            echo "❌ 后端健康检查失败"
            docker compose logs backend
            exit 1
          fi
          
          if curl -f http://localhost:3000; then
            echo "✅ 前端健康检查通过"
          else
            echo "❌ 前端健康检查失败"
            docker compose logs frontend
            exit 1
          fi
          
          echo "🎉 部署成功！"
          echo "🌍 应用地址: http://${{ secrets.EC2_HOST }}"

    - name: 发送部署通知
      if: always()
      run: |
        if [ "${{ job.status }}" == "success" ]; then
          echo "✅ 部署成功到 http://${{ secrets.EC2_HOST }}"
        else
          echo "❌ 部署失败，请检查日志"
        fi
```

---

## 🐳 Docker配置

创建或更新项目根目录的 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  # PostgreSQL数据库
  database:
    image: postgres:15
    container_name: mini-ups-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-ups_db}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-abc123}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./database/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "5432:5432"
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres}" ]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: mini-ups-redis
    command: redis-server --requirepass ${REDIS_PASSWORD:-abc123}
    ports:
      - "6379:6379"
    healthcheck:
      test: [ "CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-abc123}", "ping" ]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot后端
  backend:
    build:
      context: ../backend
      dockerfile: ../backend/Dockerfile
    container_name: mini-ups-backend
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-production}
      DATABASE_URL: jdbc:postgresql://database:5432/${POSTGRES_DB:-ups_db}
      DATABASE_USERNAME: ${POSTGRES_USER:-postgres}
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD:-abc123}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-abc123}
      JWT_SECRET: ${JWT_SECRET:-default-jwt-secret}
      WORLD_SIMULATOR_HOST: ${WORLD_SIMULATOR_HOST:-localhost}
      AMAZON_API_URL: ${AMAZON_API_URL:-http://localhost:8080}
    ports:
      - "8081:8081"
    depends_on:
      database:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:8081/api/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3

  # React前端
  frontend:
    build:
      context: ../frontend
      dockerfile: ../frontend/Dockerfile
    container_name: mini-ups-frontend
    environment:
      REACT_APP_API_URL: http://localhost:8081/api
    ports:
      - "3000:80"
    depends_on:
      - backend
    healthcheck:
      test: [ "CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:80" ]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:

networks:
  default:
    name: mini-ups-network
```

---

## 📝 部署脚本

创建文件: `scripts/deploy.sh`

```bash
#!/bin/bash

# Mini-UPS 本地部署脚本
set -e

echo "🚀 开始部署 Mini-UPS..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请启动Docker"
    exit 1
fi

# 检查是否存在.env文件
if [ ! -f .env ]; then
    echo "📝 创建默认.env文件..."
    cat > .env << 'EOF'
# 数据库配置
POSTGRES_DB=ups_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password_123

# Redis配置
REDIS_PASSWORD=your_redis_password_123

# 应用配置
JWT_SECRET=your_super_secret_jwt_key_here
SPRING_PROFILES_ACTIVE=production

# 外部服务 (可选)
WORLD_SIMULATOR_HOST=localhost
AMAZON_API_URL=http://localhost:8080
EOF
    echo "⚠️  请编辑.env文件配置密码后重新运行"
    exit 1
fi

# 停止现有服务
echo "🛑 停止现有服务..."
docker compose down || true

# 清理旧镜像（可选）
echo "🧹 清理旧镜像..."
docker system prune -f || true

# 构建并启动服务
echo "🏗️  构建并启动服务..."
docker compose up --build -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 健康检查
echo "🏥 执行健康检查..."

# 检查后端
if curl -f http://localhost:8081/api/actuator/health > /dev/null 2>&1; then
    echo "✅ 后端服务正常"
else
    echo "❌ 后端服务异常，查看日志："
    docker compose logs backend
    exit 1
fi

# 检查前端
if curl -f http://localhost:3000 > /dev/null 2>&1; then
    echo "✅ 前端服务正常"
else
    echo "❌ 前端服务异常，查看日志："
    docker compose logs frontend
    exit 1
fi

# 显示服务状态
echo ""
echo "🎉 部署完成！"
echo "📊 服务状态："
docker compose ps

echo ""
echo "🌍 访问地址："
echo "  前端应用: http://localhost:3000"
echo "  后端API:  http://localhost:8081/api"
echo "  API文档:  http://localhost:8081/swagger-ui.html"

echo ""
echo "📋 管理命令："
echo "  查看日志: docker compose logs -f"
echo "  停止服务: docker compose down"
echo "  重启服务: docker compose restart"
```

创建文件: `scripts/logs.sh`

```bash
#!/bin/bash

# 查看服务日志
SERVICE=${1:-""}

if [ -z "$SERVICE" ]; then
    echo "📋 查看所有服务日志..."
    docker compose logs -f
else
    echo "📋 查看 $SERVICE 服务日志..."
    docker compose logs -f $SERVICE
fi
```

---

## 🔧 使用说明

### 首次部署

```bash
# 1. 克隆仓库到EC2服务器
git clone https://github.com/your-username/mini-ups.git
cd mini-ups

# 2. 配置环境变量
cp .env.example .env
vim .env  # 编辑配置

# 3. 执行部署
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

### 日常操作

```bash
# 查看服务状态
docker compose ps

# 查看所有日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f backend
docker compose logs -f frontend

# 重启服务
docker compose restart

# 更新应用 (拉取最新代码)
git pull origin main
docker compose up --build -d

# 停止服务
docker compose down
```

### 故障排除

```bash
# 检查服务健康状态
curl http://localhost:8081/api/actuator/health
curl http://localhost:3000

# 进入容器调试
docker exec -it mini-ups-backend bash
docker exec -it mini-ups-postgres psql -U postgres -d ups_db

# 清理重建
docker compose down
docker system prune -a -f
docker compose up --build -d
```

---

## 💰 成本估算

**AWS免费套餐 (前12个月):**
- EC2 t3.micro: $0/月
- 总计: **$0/月**

**超出免费套餐后:**
- EC2 t3.micro: ~$6/月
- 总计: **$6/月**

**相比复杂方案节省:**
- 原企业级方案: $130-210/月
- 节省: **$124-204/月** (95%+)

---

## 🎯 特性对比

| 特性 | 简化方案 | 企业级方案 |
|------|---------|----------|
| 成本 | $0-6/月 | $130-210/月 |
| 复杂度 | 低 | 高 |
| 维护难度 | 简单 | 复杂 |
| 扩展性 | 有限 | 自动扩展 |
| 高可用 | 无 | 多AZ |
| 监控 | 基础 | 完整 |
| 适用场景 | 个人/学习/小项目 | 生产环境 |

---

## 🚀 升级路径

当项目增长需要更强能力时，可以逐步升级：

1. **添加域名和SSL**: Route 53 + CloudFront
2. **数据库升级**: RDS PostgreSQL
3. **缓存升级**: ElastiCache Redis
4. **容器编排**: ECS或EKS
5. **监控告警**: CloudWatch完整方案
6. **CI/CD增强**: 多环境部署

---

## ✅ 总结

这个简化方案提供了：
- ✅ 自动化CI/CD部署
- ✅ 完整的开发工作流
- ✅ 生产级别的容器化
- ✅ 健康检查和监控
- ✅ 极低的运维成本

适合个人项目、学习环境和小型应用的快速部署和迭代。