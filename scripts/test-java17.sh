#!/bin/bash

# Set JAVA_HOME to Java 17 for testing
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

echo "Using Java version:"
$JAVA_HOME/bin/java -version

echo ""
echo "Running tests with Java 17..."
cd "$(dirname "$0")/../backend"

# Set Maven options for Java 17 compatibility
export MAVEN_OPTS="-Xmx1024m -XX:+UseG1GC -Djdk.instrument.traceUsage=false --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED"

# Run tests with proper Java version
mvn clean test -Dspring.profiles.active=test -DfailIfNoTests=false "$@"