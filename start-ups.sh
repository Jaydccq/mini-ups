#!/bin/bash

# UPS服务启动脚本

echo "🚀 启动Mini-UPS服务..."

# 检测系统架构
ARCH=$(uname -m)
COMPOSE_FILE="docker-compose.yml"

if [[ "$ARCH" == "arm64" ]]; then
    echo "🍎 检测到Apple Silicon (M1/M2) 架构，使用优化配置..."
    COMPOSE_FILE="docker-compose.m1.yml"
else
    echo "💻 检测到 $ARCH 架构，使用标准配置..."
fi

# 检查是否已存在projectnet网络，如果不存在则创建
if ! docker network ls | grep -q projectnet; then
    echo "📡 创建projectnet网络..."
    docker network create projectnet
else
    echo "📡 projectnet网络已存在"
fi

# 更新环境变量
export WORLD_SIMULATOR_HOST=docker_deploy-server-1
export AMAZON_BASE_URL=http://mini-amazon-web:8080

echo "🔧 环境配置:"
echo "  - 系统架构: $ARCH"
echo "  - 配置文件: $COMPOSE_FILE"
echo "  - 世界模拟器: ${WORLD_SIMULATOR_HOST}:12345"
echo "  - Amazon服务: ${AMAZON_BASE_URL}"

# 启动UPS服务
echo "🐳 启动Docker容器..."
docker compose -f $COMPOSE_FILE up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo "📊 检查服务状态:"
docker compose ps

echo ""
echo "✅ UPS服务启动完成!"
echo ""
echo "🌐 访问地址:"
echo "  - UPS前端: http://localhost:3000"
echo "  - UPS后端API: http://localhost:8081"
echo "  - API文档: http://localhost:8081/swagger-ui.html"
echo "  - 数据库: localhost:5433"
echo "  - Redis: localhost:6380"
echo ""
echo "📝 注意事项:"
echo "  - 请确保世界模拟器和Amazon服务已经启动"
echo "  - 检查所有服务是否在同一个projectnet网络中"
echo ""
echo "🔍 查看日志: docker compose logs -f [service_name]"
echo "🛑 停止服务: docker compose down"