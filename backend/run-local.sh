#!/bin/bash

echo "=== Starting Mini-UPS Backend (Local Development) ==="

# Check if PostgreSQL is running
if ! pg_isready -h localhost -p 5432 2>/dev/null; then
    echo "❌ PostgreSQL is not running on localhost:5432"
    echo "Please start PostgreSQL first and create the database:"
    echo "  createdb ups_db"
    exit 1
fi

# Check if Redis is running  
if ! redis-cli -p 6380 ping 2>/dev/null; then
    echo "❌ Redis is not running on localhost:6380"
    echo "Please start Redis first: redis-server --port 6380"
    exit 1
fi

echo "✅ PostgreSQL and Redis are running"

# Set development environment
export SPRING_PROFILES_ACTIVE=local
export DATABASE_URL=jdbc:postgresql://localhost:5432/ups_db
export JWT_SECRET=your-very-long-secret-key-for-jwt-signing-should-be-at-least-256-bits-long

echo "🚀 Starting Spring Boot application..."
echo "📍 Profile: local"
echo "🗄️  Database: postgresql://localhost:5432/ups_db"
echo "📦 Redis: localhost:6380"
echo "🌐 Server: http://localhost:8081/api"
echo ""

./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="--add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED"