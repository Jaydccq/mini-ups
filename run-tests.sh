#!/bin/bash

# Mini-UPS 测试运行脚本
# 使用Java 17运行测试，避免版本兼容性问题

# 设置颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 设置Java环境
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH

echo -e "${BLUE}🚀 Mini-UPS 测试运行器${NC}"
echo -e "${BLUE}================================${NC}"

# 检查Java版本
echo -e "${YELLOW}📋 检查Java版本...${NC}"
java -version

# 进入backend目录
cd "$(dirname "$0")/backend"

# 显示菜单
echo -e "${BLUE}请选择要运行的测试类型：${NC}"
echo "1. 🏃‍♂️ 运行所有单元测试 (快速)"
echo "2. 🔍 运行特定测试类"
echo "3. 🌐 运行集成测试 (慢，全面)"
echo "4. 📊 运行测试并生成覆盖率报告"
echo "5. 🧹 清理并重新编译"
echo "6. ❌ 退出"

read -p "请输入选项 (1-6): " choice

case $choice in
    1)
        echo -e "${GREEN}🏃‍♂️ 运行单元测试...${NC}"
        mvn test
        ;;
    2)
        echo -e "${YELLOW}请输入测试类名 (例如: UserServiceTest):${NC}"
        read -p "测试类名: " test_class
        echo -e "${GREEN}🔍 运行测试类: $test_class${NC}"
        mvn test -Dtest=$test_class
        ;;
    3)
        echo -e "${GREEN}🌐 运行集成测试...${NC}"
        mvn verify
        ;;
    4)
        echo -e "${GREEN}📊 运行测试并生成覆盖率报告...${NC}"
        mvn clean test jacoco:report
        echo -e "${BLUE}📈 覆盖率报告已生成: target/site/jacoco/index.html${NC}"
        ;;
    5)
        echo -e "${GREEN}🧹 清理并重新编译...${NC}"
        mvn clean compile
        ;;
    6)
        echo -e "${YELLOW}👋 再见！${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}❌ 无效选项！${NC}"
        exit 1
        ;;
esac

# 检查测试结果
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 测试运行完成！${NC}"
else
    echo -e "${RED}❌ 测试运行失败！${NC}"
    echo -e "${YELLOW}💡 提示：查看上面的错误信息，或参考 JAVA_TESTING_GUIDE.md${NC}"
fi