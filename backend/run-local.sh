#!/bin/bash

# ================================================================
# Mini-UPS Backend - Local Development Runner
# ================================================================
# This script starts the Spring Boot backend in local development mode.
# It expects infrastructure services (PostgreSQL, Redis, RabbitMQ) to be
# running via docker-compose.local.yml
# ================================================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Starting Mini-UPS Backend (Local Development) ==="
echo ""

# Function to check if a service is available
check_service() {
    local name=$1
    local host=$2
    local port=$3
    local max_attempts=${4:-30}
    local attempt=1
    
    echo -n "⏳ Checking $name ($host:$port)... "
    while ! nc -z "$host" "$port" 2>/dev/null; do
        if [ $attempt -ge $max_attempts ]; then
            echo "❌ FAILED"
            return 1
        fi
        attempt=$((attempt + 1))
        sleep 1
    done
    echo "✅ OK"
    return 0
}

# Check if Docker infrastructure services are running
echo "📦 Checking infrastructure services..."
echo ""

SERVICES_MISSING=false

# Check PostgreSQL
if ! check_service "PostgreSQL" "localhost" "5432" 3; then
    SERVICES_MISSING=true
fi

# Check Redis
if ! check_service "Redis" "localhost" "6379" 3; then
    SERVICES_MISSING=true
fi

# Check RabbitMQ
if ! check_service "RabbitMQ" "localhost" "5672" 3; then
    SERVICES_MISSING=true
fi

echo ""

# If services are missing, offer to start them
if [ "$SERVICES_MISSING" = true ]; then
    echo "❌ Some infrastructure services are not running."
    echo ""
    echo "💡 To start them, run:"
    echo "   cd $PROJECT_ROOT"
    echo "   docker compose -f docker-compose.local.yml up -d"
    echo ""
    
    # Check if user wants to auto-start
    if [ "$1" = "--auto-start" ] || [ "$1" = "-a" ]; then
        echo "🚀 Auto-starting infrastructure services..."
        cd "$PROJECT_ROOT"
        docker compose -f docker-compose.local.yml up -d
        
        echo ""
        echo "⏳ Waiting for services to be ready..."
        sleep 5
        
        # Re-check services
        check_service "PostgreSQL" "localhost" "5432" 30 || exit 1
        check_service "Redis" "localhost" "6379" 30 || exit 1
        check_service "RabbitMQ" "localhost" "5672" 30 || exit 1
        echo ""
    else
        echo "💡 Or run this script with --auto-start (-a) to start services automatically."
        exit 1
    fi
fi

echo "✅ All infrastructure services are running"
echo ""

# Set environment variables
export SPRING_PROFILES_ACTIVE=local
export JWT_SECRET="${JWT_SECRET:-your-very-long-secret-key-for-jwt-signing-should-be-at-least-256-bits-long-for-local-dev}"

# Print startup info
echo "🚀 Starting Spring Boot application..."
echo ""
echo "📋 Configuration:"
echo "   Profile:    local"
echo "   Database:   postgresql://localhost:5432/ups_db"
echo "   Redis:      localhost:6379"
echo "   RabbitMQ:   localhost:5672"
echo "   Server:     http://localhost:8081"
echo "   Swagger:    http://localhost:8081/swagger-ui.html"
echo ""

# Change to backend directory
cd "$BACKEND_DIR"

# Run Spring Boot with local profile
./mvnw spring-boot:run \
    -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.jvmArguments="--add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED"