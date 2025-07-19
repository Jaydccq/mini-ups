#!/bin/bash

# 用户模块API测试运行脚本
echo "🧪 开始运行用户模块API测试..."

cd backend

# 编译项目
echo "📦 编译项目..."
mvn clean compile test-compile -q

# 创建测试类列表
USER_TEST_CLASSES=(
    "com.miniups.controller.UserControllerTest"
    "com.miniups.controller.AuthControllerTest"
    "com.miniups.service.UserServiceTest"
    "com.miniups.security.CustomUserDetailsServiceTest"
)

# 运行每个测试类
for test_class in "${USER_TEST_CLASSES[@]}"; do
    echo "🔍 运行测试: $test_class"
    if mvn test -Dtest="$test_class" -q; then
        echo "✅ $test_class - 通过"
    else
        echo "❌ $test_class - 失败"
        echo "查看详细错误信息，请运行: mvn test -Dtest=$test_class"
    fi
    echo ""
done

echo "🏁 用户模块测试完成!"
echo ""
echo "💡 要查看测试覆盖率报告，请运行:"
echo "   mvn jacoco:report"
echo "   open target/site/jacoco/index.html"