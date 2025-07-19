#!/bin/bash

# Mini-UPS 停止脚本
# 使用此脚本可以停止前端和后端服务

echo "🛑 停止 Mini-UPS 前后端服务..."

# 从PID文件读取进程ID并停止
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        echo "停止后端进程: $BACKEND_PID"
        kill $BACKEND_PID
        sleep 2
        if kill -0 $BACKEND_PID 2>/dev/null; then
            echo "强制停止后端进程"
            kill -9 $BACKEND_PID
        fi
    else
        echo "后端进程已经停止"
    fi
    rm -f logs/backend.pid
else
    echo "未找到后端PID文件，尝试通过端口杀死进程"
    lsof -ti :8081 | xargs kill -9 2>/dev/null || true
fi

if [ -f "logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        echo "停止前端进程: $FRONTEND_PID"
        kill $FRONTEND_PID
        sleep 2
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            echo "强制停止前端进程"
            kill -9 $FRONTEND_PID
        fi
    else
        echo "前端进程已经停止"
    fi
    rm -f logs/frontend.pid
else
    echo "未找到前端PID文件，尝试通过端口杀死进程"
    lsof -ti :3001 | xargs kill -9 2>/dev/null || true
fi

# 清理其他可能的进程
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "vite" 2>/dev/null || true

echo "✅ 所有服务已停止"