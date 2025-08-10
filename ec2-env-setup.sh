#!/bin/bash

# EC2 环境配置脚本 - 为 Jaydccq 定制
# 在 EC2 上执行此脚本来创建环境配置文件

echo "🔧 为用户 Jaydccq 创建 Mini-UPS 环境配置..."

# 创建 .env 文件
cat > .env << 'EOF'
# Database Configuration (使用原有密码)
POSTGRES_DB=ups_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=abc123

# Redis Configuration (使用原有密码)
REDIS_PASSWORD=test123

# Application Configuration
JWT_SECRET=mini-ups-production-jwt-secret-key-32-chars-minimum-length-for-security
SPRING_PROFILES_ACTIVE=production

# Docker Images (由 CI/CD 自动更新)
BACKEND_IMAGE=ghcr.io/jaydccq/mini-ups/backend:latest
FRONTEND_IMAGE=ghcr.io/jaydccq/mini-ups/frontend:latest

# External Access
PUBLIC_IP=44.219.181.190
VITE_API_BASE_URL=http://44.219.181.190:8081/api
VITE_WS_BASE_URL=ws://44.219.181.190:8081/ws

# Memory Optimization for t2.micro
JAVA_OPTS=-Xmx300m -Xms150m -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Feature Flags (disable resource-intensive features for t2.micro)
WORLD_SIMULATOR_ENABLED=false
RABBITMQ_ENABLED=false
NOTIFICATIONS_ENABLED=false
ANALYTICS_ENABLED=false
EMAIL_NOTIFICATIONS_ENABLED=false
SMS_NOTIFICATIONS_ENABLED=false
PUSH_NOTIFICATIONS_ENABLED=false

# Environment
ENVIRONMENT=production
NODE_ENV=production
EOF

echo "✅ 环境配置文件 .env 已创建"

# 创建必要的目录
mkdir -p logs database nginx

# 创建基础数据库初始化文件（如果不存在）
if [ ! -f database/init.sql ]; then
    cat > database/init.sql << 'INITEOF'
-- Basic database initialization for Mini-UPS
-- Spring Boot JPA will handle the actual table creation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- This ensures database is ready for Spring Boot initialization
SELECT 'Mini-UPS database initialized' as status;
INITEOF
    echo "✅ 数据库初始化文件已创建"
fi

# 复制 nginx 配置（如果存在的话）
if [ -f nginx.conf ] && [ ! -f nginx/nginx.conf ]; then
    cp nginx.conf nginx/
    echo "✅ Nginx 配置文件已复制"
fi

echo ""
echo "🎉 环境配置完成！"
echo ""
echo "📋 下一步："
echo "1. 确保已登录 GitHub Container Registry:"
echo "   echo 'YOUR_GITHUB_TOKEN' | docker login ghcr.io -u Jaydccq --password-stdin"
echo ""
echo "2. 启动服务:"
echo "   docker compose -f docker-compose.t2micro.yml up -d"
echo ""
echo "3. 检查状态:"
echo "   docker compose -f docker-compose.t2micro.yml ps"
echo "   curl http://localhost:8081/actuator/health"
echo "   curl http://localhost:3000"
echo ""
echo "🌐 访问地址:"
echo "   Frontend: http://44.219.181.190:3000"
echo "   Backend:  http://44.219.181.190:8081/api"
echo "   Health:   http://44.219.181.190:8081/actuator/health"