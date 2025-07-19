#!/bin/bash

# 单独启动后端服务
echo "🔧 启动后端服务..."

cd backend

# 检查端口是否被占用
if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null; then
    echo "⚠️  端口 8081 已被占用，正在停止..."
    lsof -ti :8081 | xargs kill -9
    sleep 2
fi

# 启动后端
echo "启动 Spring Boot 应用..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true