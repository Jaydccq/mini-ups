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

# 等待后端服务启动
echo "⏳ 等待后端服务启动..."
BACKEND_READY=false
for i in {1..20}; do
    if curl -fs http://localhost:8081/api/actuator/health > /dev/null 2>&1; then
        echo "✅ 后端服务正常"
        BACKEND_READY=true
        break
    fi
    echo "后端未就绪，等待5秒... ($i/20)"
    sleep 5
done

if [ "$BACKEND_READY" != "true" ]; then
    echo "❌ 后端服务异常，查看日志："
    docker compose logs backend
    exit 1
fi

# 等待前端服务启动
echo "⏳ 等待前端服务启动..."
FRONTEND_READY=false
for i in {1..20}; do
    if curl -fs http://localhost:3000 > /dev/null 2>&1; then
        echo "✅ 前端服务正常"
        FRONTEND_READY=true
        break
    fi
    echo "前端未就绪，等待5秒... ($i/20)"
    sleep 5
done

if [ "$FRONTEND_READY" != "true" ]; then
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