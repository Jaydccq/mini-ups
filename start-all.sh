#!/bin/bash

# Mini-UPS 前后端同时启动脚本
# 使用此脚本可以同时启动前端和后端服务

echo "🚀 启动 Mini-UPS 前后端服务..."

# 检查依赖
check_dependency() {
    if ! command -v $1 &> /dev/null; then
        echo "❌ 错误: $1 未安装，请先安装 $1"
        exit 1
    fi
}

echo "📋 检查依赖..."
check_dependency "npm"
check_dependency "mvn"
check_dependency "java"

# 创建日志目录
mkdir -p logs

# 启动后端
echo "🔧 启动后端服务 (Spring Boot)..."
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "后端进程 PID: $BACKEND_PID"

# 等待后端启动
echo "⏳ 等待后端启动..."
for i in {1..20}; do
    if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
        echo "✅ 后端启动成功: http://localhost:8081"
        break
    fi
    if [ $i -eq 20 ]; then
        echo "❌ 后端启动超时，请检查日志: logs/backend.log"
        kill $BACKEND_PID
        exit 1
    fi
    echo "等待后端启动... ($i/20)"
    sleep 3
done

# 启动前端
echo "🎨 启动前端服务 (React + Vite)..."
cd ../frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "前端进程 PID: $FRONTEND_PID"

# 等待前端启动
echo "⏳ 等待前端启动..."
for i in {1..15}; do
    if curl -s http://localhost:3001 > /dev/null 2>&1; then
        echo "✅ 前端启动成功: http://localhost:3001"
        break
    fi
    if [ $i -eq 15 ]; then
        echo "❌ 前端启动超时，请检查日志: logs/frontend.log"
        kill $BACKEND_PID $FRONTEND_PID
        exit 1
    fi
    echo "等待前端启动... ($i/15)"
    sleep 2
done

# 保存进程ID
echo $BACKEND_PID > ../logs/backend.pid
echo $FRONTEND_PID > ../logs/frontend.pid

echo ""
echo "🎉 Mini-UPS 启动成功！"
echo ""
echo "📱 前端网页: http://localhost:3001"
echo "🔧 后端API:  http://localhost:8081"
echo "📖 API文档:  http://localhost:8081/swagger-ui.html"
echo ""
echo "📋 服务状态:"
echo "   前端进程: $FRONTEND_PID"
echo "   后端进程: $BACKEND_PID"
echo ""
echo "📝 日志文件:"
echo "   前端日志: logs/frontend.log"
echo "   后端日志: logs/backend.log"
echo ""
echo "⚠️  要停止服务，请运行: ./stop-all.sh"
echo ""
echo "🔥 按 Ctrl+C 可以停止此脚本，但服务会继续运行"

# 保持脚本运行并监控服务
trap 'echo "正在停止服务..."; kill $BACKEND_PID $FRONTEND_PID; exit' INT

echo "监控服务状态... (按 Ctrl+C 停止)"
while true; do
    sleep 10
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo "❌ 后端进程已停止"
        break
    fi
    if ! kill -0 $FRONTEND_PID 2>/dev/null; then
        echo "❌ 前端进程已停止"
        break
    fi
done