#!/bin/bash

# 单独启动前端服务
echo "🎨 启动前端服务..."

cd frontend

# 检查端口是否被占用
if lsof -Pi :3001 -sTCP:LISTEN -t >/dev/null; then
    echo "⚠️  端口 3001 已被占用，正在停止..."
    lsof -ti :3001 | xargs kill -9
    sleep 2
fi

# 启动前端
echo "启动 React + Vite 开发服务器..."
npm run dev