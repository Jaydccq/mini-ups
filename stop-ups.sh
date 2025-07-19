#!/bin/bash

# UPS服务停止脚本

echo "🛑 停止Mini-UPS服务..."

# 检测系统架构
ARCH=$(uname -m)
COMPOSE_FILE="docker-compose.yml"

if [[ "$ARCH" == "arm64" ]]; then
    echo "🍎 检测到Apple Silicon架构..."
    COMPOSE_FILE="docker-compose.m1.yml"
fi

# 停止所有容器
docker compose -f $COMPOSE_FILE down

echo "🧹 清理资源..."

# 可选：删除数据卷（谨慎使用）
read -p "是否要删除数据卷？这将清除所有数据 (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️ 删除数据卷..."
    docker compose down -v
    docker volume prune -f
fi

# 可选：删除镜像
read -p "是否要删除UPS相关镜像？ (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️ 删除镜像..."
    docker rmi $(docker images | grep mini-ups | awk '{print $3}') 2>/dev/null || true
fi

echo "✅ UPS服务已停止"