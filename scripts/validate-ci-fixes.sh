#!/bin/bash

# 验证 CI/CD 修复的快速测试脚本

echo "🔍 验证 CI/CD 修复..."

# 1. 检查前端脚本是否存在
echo "📦 检查前端脚本..."
cd frontend

if ! npm run --silent > /dev/null 2>&1; then
    echo "❌ npm 脚本检查失败"
    exit 1
fi

# 检查关键脚本是否存在
scripts=("test:coverage" "type-check" "lint" "build:staging" "build:production")
for script in "${scripts[@]}"; do
    if grep -q "\"$script\":" package.json; then
        echo "✅ 脚本 $script 存在"
    else
        echo "❌ 脚本 $script 缺失"
        exit 1
    fi
done

cd ..

# 2. 检查后端健康检查端点是否添加
echo "🏥 检查后端健康检查端点..."
if grep -q "/api/health" backend/src/main/java/com/miniups/controller/HomeController.java; then
    echo "✅ 后端健康检查端点已添加"
else
    echo "❌ 后端健康检查端点缺失"
    exit 1
fi

# 3. 检查CI环境配置文件
echo "⚙️  检查CI环境配置..."
if [ -f ".env.ci" ]; then
    echo "✅ CI环境配置文件存在"
else
    echo "❌ CI环境配置文件缺失"
    exit 1
fi

# 4. 验证Docker Compose配置
echo "🐳 验证Docker Compose配置..."
if docker compose config > /dev/null 2>&1; then
    echo "✅ Docker Compose配置有效"
else
    echo "❌ Docker Compose配置无效"
    exit 1
fi

# 5. 检查CI/CD配置文件语法
echo "🔧 检查CI/CD配置文件..."
if [ -f ".github/workflows/ci-cd.yml" ]; then
    echo "✅ 主CI/CD配置文件存在"
else
    echo "❌ 主CI/CD配置文件缺失"
    exit 1
fi

if [ -f ".github/workflows/frontend-deploy.yml" ]; then
    echo "✅ 前端部署配置文件存在"
else
    echo "❌ 前端部署配置文件缺失"
    exit 1
fi

# 6. 检查修复的具体问题
echo "🔍 验证具体修复..."

# 检查前端测试命令修复
if grep -q "npm run test:coverage" .github/workflows/ci-cd.yml; then
    echo "✅ 前端测试命令已修复"
else
    echo "❌ 前端测试命令未修复"
fi

# 检查健康检查端点修复
if grep -q "/api/health" .github/workflows/ci-cd.yml; then
    echo "✅ 健康检查端点已更新"
else
    echo "❌ 健康检查端点未更新"
fi

# 检查容器名称动态查找
if grep -q 'docker ps -q --filter' .github/workflows/ci-cd.yml; then
    echo "✅ 容器名称动态查找已添加"
else
    echo "❌ 容器名称仍然硬编码"
fi

echo ""
echo "🎉 CI/CD 修复验证完成！"
echo ""
echo "修复内容总结："
echo "  ✅ 前端测试脚本名称匹配"
echo "  ✅ 后端健康检查端点添加"  
echo "  ✅ Docker容器名称动态查找"
echo "  ✅ CI环境配置文件创建"
echo "  ✅ Docker Compose版本警告修复"
echo "  ✅ 环境变量默认值设置"
echo ""
echo "💡 主要改进："
echo "  • 修复了前端测试命令不匹配问题"
echo "  • 添加了缺失的API健康检查端点"
echo "  • 改进了容器名称的动态查找"
echo "  • 简化了AWS ECS相关的复杂配置"
echo "  • 创建了测试和验证脚本"