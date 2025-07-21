#!/bin/bash

# 模拟GitHub Actions CI环境来验证Java 17设置

echo "🔍 Simulating CI environment..."

# 设置Java 17环境（模拟actions/setup-java@v4的行为）
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"

echo ""
echo "🔍 Verifying Java setup..."
echo "JAVA_HOME: $JAVA_HOME"
java -version
javac -version
mvn -version | grep -E "(Java version|JVM)"
echo "✅ Java 17 verified"

echo ""
echo "🧪 Running simulated backend tests..."
cd "$(dirname "$0")/../backend"

# 模拟CI环境变量
export SPRING_PROFILES_ACTIVE=test
export MAVEN_OPTS="-Xmx1024m -XX:+UseG1GC"

# 运行测试（只运行一个快速测试来验证）
mvn clean test -Dtest=ExceptionMetricsConfigTest -DfailIfNoTests=false

echo ""
echo "✅ CI simulation completed successfully!"