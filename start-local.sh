#!/bin/bash

echo "=== Mini-UPS Local Development Setup ==="

# Check if PostgreSQL is running
if ! pg_isready -h localhost -p 5432 2>/dev/null; then
    echo "❌ PostgreSQL is not running on localhost:5432 (redis-server --port 6380 --daemonize yes)"
    echo "Please start PostgreSQL first:"
    echo "  - macOS: brew services start postgresql"
    echo "  - Ubuntu: sudo systemctl start postgresql"
    echo "  - Windows: Start PostgreSQL service"
    exit 1
fi

# Check if Redis is running
if ! redis-cli -p 6380 ping 2>/dev/null; then
    echo "❌ Redis is not running on localhost:6380"
    echo "Please start Redis first:"
    echo "  - macOS: brew services start redis"
    echo "  - Ubuntu: sudo systemctl start redis"
    echo "  - Windows: Start Redis service"
    exit 1
fi

echo "✅ PostgreSQL and Redis are running"

# Create databases if they don't exist
echo "📊 Setting up databases..."
createdb ups_db 2>/dev/null || echo "Database 'ups_db' already exists"

# Set environment variables
export SPRING_PROFILES_ACTIVE=local
export DATABASE_URL=jdbc:postgresql://localhost:5432/ups_db
export REDIS_HOST=localhost
export REDIS_PORT=6380
export JWT_SECRET=your-very-long-secret-key-for-jwt-signing-should-be-at-least-256-bits-long

echo "🚀 Starting services..."

# Start backend in background
echo "Starting Spring Boot backend..."
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="--add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED" &
BACKEND_PID=$!
cd ..

# Wait for backend to start
echo "Waiting for backend to start..."
sleep 10

# Start frontend
echo "Starting React frontend..."
cd frontend
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "=== Services Started ==="
echo "🔧 Backend:  http://localhost:8081"
echo "🌐 Frontend: http://localhost:3001"
echo "🗄️  Database: postgresql://localhost:5432/ups_db"
echo "📦 Redis:    localhost:6380"
echo ""
echo "Press Ctrl+C to stop all services"

# Function to cleanup processes
cleanup() {
    echo ""
    echo "Stopping services..."
    kill $BACKEND_PID 2>/dev/null
    kill $FRONTEND_PID 2>/dev/null
    exit 0
}

# Set trap to cleanup on script termination
trap cleanup SIGINT SIGTERM

# Wait for background processes
wait