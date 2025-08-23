#!/bin/bash

echo "🚀 Starting Mini-UPS Complete System..."
echo "========================================="

# Create projectnet network if it doesn't exist
echo "📡 Creating Docker network..."
docker network create projectnet 2>/dev/null || echo "Network 'projectnet' already exists"

# Start all services
echo "🐳 Starting all containers..."
docker compose up --build -d

echo ""
echo "✅ Services started successfully!"
echo ""
echo "📋 Service Information:"
echo "└── 📦 UPS Core:"
echo "    ├── Frontend: http://localhost:3000"
echo "    ├── Backend: http://localhost:8081"
echo "    ├── Database: localhost:5431"
echo "    ├── Redis: localhost:6380"
echo "    └── RabbitMQ: localhost:15672"
echo ""
echo "🔍 Checking service status..."
docker compose ps

echo ""
echo "📊 To view logs:"
echo "├── All services: docker compose logs -f"
echo "├── UPS Backend: docker compose logs -f ups-backend"
echo "├── UPS Frontend: docker compose logs -f ups-frontend"
echo "└── Database/Cache/MQ: docker compose logs -f ups-database redis rabbitmq"
echo ""
echo "⏹️  To stop all services:"
echo "└── ./stop-all.sh or docker compose down"
