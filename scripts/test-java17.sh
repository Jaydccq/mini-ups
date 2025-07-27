#!/bin/bash

# Set JAVA_HOME to Java 17 for testing
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

echo "Using Java version:"
$JAVA_HOME/bin/java -version

echo ""
echo "Running tests with Java 17..."
cd "$(dirname "$0")/../backend"

# Enhanced Maven options for Java 17+ compatibility
export MAVEN_OPTS="-Xmx2048m -XX:+UseG1GC -XX:+EnableDynamicAgentLoading -Djdk.instrument.traceUsage=false -Dnet.bytebuddy.experimental=true --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/sun.util.calendar=ALL-UNNAMED --add-opens java.base/sun.util.locale=ALL-UNNAMED --add-opens java.base/sun.util.resources=ALL-UNNAMED --add-opens java.base/sun.util.cldr=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.logging/java.util.logging=ALL-UNNAMED --enable-native-access=ALL-UNNAMED"

# Run tests with enhanced compatibility options and JaCoCo disabled for CI compatibility
mvn clean test \
  -Dspring.profiles.active=test \
  -DfailIfNoTests=false \
  -Dfile.encoding=UTF-8 \
  -Dmockito.mock-maker.inline=true \
  -Djunit.platform.output.capture.stdout=true \
  -Djunit.platform.output.capture.stderr=true \
  -Djacoco.skip=true \
  "$@"