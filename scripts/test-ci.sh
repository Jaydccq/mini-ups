#!/bin/bash

# Mini-UPS CI/CD 本地测试脚本
# 模拟 GitHub Actions 的构建和测试流程

set -e  # 遇到错误立即退出

echo "🚀 开始本地 CI/CD 测试..."

# 检查必要工具
echo "📋 检查必要工具..."
command -v docker >/dev/null 2>&1 || { echo "❌ Docker 未安装！"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "❌ Node.js 未安装！"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven 未安装！"; exit 1; }

# 复制CI环境配置
echo "⚙️  设置测试环境..."
cp .env.ci .env

# 启动测试数据库服务
echo "🐘 启动测试数据库..."
docker compose up -d database redis rabbitmq
sleep 15

# 等待服务启动
echo "⏳ 等待数据库服务启动..."
docker compose exec -T database pg_isready -U postgres || {
    echo "❌ 数据库启动失败！"
    docker compose logs database
    exit 1
}

echo "✅ 数据库服务已就绪"

# 后端测试
echo "🔧 运行后端测试..."
cd backend
mvn clean test -Dspring.profiles.active=test \
    -Dspring.datasource.url=jdbc:postgresql://localhost:5432/ups_db_test \
    -Dspring.datasource.username=postgres \
    -Dspring.datasource.password=abc123 \
    -Dspring.data.redis.host=localhost \
    -Dspring.data.redis.port=6379

if [ $? -eq 0 ]; then
    echo "✅ 后端测试通过"
else
    echo "❌ 后端测试失败"
    cd ..
    docker compose down
    exit 1
fi

cd ..

# 前端测试
echo "🎨 运行前端测试..."
cd frontend

# 安装依赖
npm ci --prefer-offline --no-audit

# 类型检查
npm run type-check
if [ $? -eq 0 ]; then
    echo "✅ TypeScript 类型检查通过"
else
    echo "❌ TypeScript 类型检查失败"
    cd ..
    docker compose down
    exit 1
fi

# 代码检查
npm run lint
if [ $? -eq 0 ]; then
    echo "✅ ESLint 代码检查通过"
else
    echo "❌ ESLint 代码检查失败"
    cd ..
    docker compose down
    exit 1
fi

# 运行测试
npm run test:coverage
if [ $? -eq 0 ]; then
    echo "✅ 前端测试通过"
else
    echo "❌ 前端测试失败"
    cd ..
    docker compose down
    exit 1
fi

# 构建前端
npm run build:staging
if [ $? -eq 0 ]; then
    echo "✅ 前端构建成功"
else
    echo "❌ 前端构建失败"
    cd ..
    docker compose down
    exit 1
fi

cd ..

# Docker 构建测试
echo "🐳 测试 Docker 构建..."
docker compose build --no-cache
if [ $? -eq 0 ]; then
    echo "✅ Docker 镜像构建成功"
else
    echo "❌ Docker 镜像构建失败"
    docker compose down
    exit 1
fi

# 启动完整服务
echo "🚀 启动完整服务..."
docker compose up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 健康检查
echo "🔍 执行健康检查..."

# 检查后端
if curl -f http://localhost:8081/api/health > /dev/null 2>&1; then
    echo "✅ 后端服务健康检查通过"
else
    echo "❌ 后端服务健康检查失败"
    docker compose logs backend
    docker compose down
    exit 1
fi

# 检查前端
if curl -f http://localhost:3000 > /dev/null 2>&1; then
    echo "✅ 前端服务健康检查通过"
else
    echo "❌ 前端服务健康检查失败"
    docker compose logs frontend
    docker compose down
    exit 1
fi

# 检查数据库连接
if docker exec mini-ups-postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo "✅ 数据库连接检查通过"
else
    echo "❌ 数据库连接检查失败"
    docker compose down
    exit 1
fi

# 检查Redis连接
if docker exec mini-ups-redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis连接检查通过"
else
    echo "❌ Redis连接检查失败"
    docker compose down
    exit 1
fi

echo ""
echo "🎉 所有测试通过！CI/CD 流程验证成功！"
echo ""
echo "📊 测试报告："
echo "  ✅ 后端单元测试: 通过"
echo "  ✅ 前端类型检查: 通过"
echo "  ✅ 前端代码检查: 通过"
echo "  ✅ 前端单元测试: 通过"
echo "  ✅ Docker构建: 通过"
echo "  ✅ 服务健康检查: 通过"
echo ""
echo "🔗 服务地址："
echo "  • 前端: http://localhost:3000"
echo "  • 后端API: http://localhost:8081/api"
echo "  • API文档: http://localhost:8081/swagger-ui.html"
echo "  • 健康检查: http://localhost:8081/actuator/health"
echo ""
echo "停止服务: docker compose down"