#!/bin/bash

# Set JAVA_HOME to Java 17 for testing
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

echo "Using Java version:"
$JAVA_HOME/bin/java -version

echo ""
echo "Running tests with Java 17..."
cd "$(dirname "$0")/../backend"

# Run tests with proper Java version
mvn clean test -Dspring.profiles.active=test -DfailIfNoTests=false "$@"