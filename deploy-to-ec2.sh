#!/bin/bash

# Manual deployment script for EC2
# Run this on your local machine to deploy to EC2

set -e

echo "🚀 Deploying Mini-UPS to EC2..."
echo "Target: ec2-user@44.219.181.190"
echo ""

# Check if SSH key exists
if [ ! -f ~/Downloads/mini-ups-key.pem ]; then
    echo "❌ SSH key not found at ~/Downloads/mini-ups-key.pem"
    echo "Please ensure your EC2 SSH key is available"
    exit 1
fi

# Set correct permissions for SSH key
chmod 600 ~/Downloads/mini-ups-key.pem

echo "📤 Uploading deployment files to EC2..."

# Upload docker-compose and environment files
scp -i ~/Downloads/mini-ups-key.pem \
    docker-compose.t2micro.yml \
    .env.t2micro \
    nginx/nginx.conf \
    ec2-user@44.219.181.190:/home/ec2-user/mini-ups/

echo "🔧 Executing deployment on EC2..."

# Execute deployment on EC2
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190 << 'EOF'
cd /home/ec2-user/mini-ups

echo "🐳 Setting up Docker environment..."

# Create nginx directory
mkdir -p nginx
cp nginx.conf nginx/

# Copy environment file
cp .env.t2micro .env

# Create necessary directories
mkdir -p logs database

# Create basic database initialization file if it doesn't exist
if [ ! -f database/init.sql ]; then
    cat > database/init.sql << 'INITEOF'
-- Basic database initialization for Mini-UPS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create basic tables will be handled by Spring Boot JPA
-- This file ensures the database is ready
INITEOF
fi

echo "🚀 Starting Mini-UPS services..."

# Stop any existing services
docker compose -f docker-compose.t2micro.yml down || true

# Pull latest images (if using remote images)
echo "📥 Pulling Docker images..."
docker compose -f docker-compose.t2micro.yml pull || echo "⚠️  Could not pull images, will build locally if needed"

# Start services
echo "🔄 Starting services..."
docker compose -f docker-compose.t2micro.yml up -d

echo "⏳ Waiting for services to be healthy..."
sleep 30

# Health check
echo "🏥 Performing health checks..."

for i in {1..10}; do
    if docker compose -f docker-compose.t2micro.yml ps | grep -q "healthy"; then
        echo "✅ Services are healthy!"
        break
    fi
    echo "⏳ Waiting for services to be healthy... (attempt $i/10)"
    sleep 10
done

# Show running services
echo "📊 Service Status:"
docker compose -f docker-compose.t2micro.yml ps

echo ""
echo "🌐 Access URLs:"
echo "Frontend: http://44.219.181.190:3000"
echo "Backend API: http://44.219.181.190:8081/api"
echo "Health Check: http://44.219.181.190:8081/actuator/health"

if docker compose -f docker-compose.t2micro.yml ps | grep -q "nginx"; then
    echo "Nginx Proxy: http://44.219.181.190 (if nginx is running)"
fi

echo ""
echo "🎉 Deployment completed!"
EOF

echo ""
echo "✅ Deployment script finished!"
echo ""
echo "🌐 Your application should now be accessible at:"
echo "   Frontend: http://44.219.181.190:3000"
echo "   Backend:  http://44.219.181.190:8081/api"
echo ""
echo "🔍 To check status, run:"
echo "   ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190 'cd mini-ups && docker compose -f docker-compose.t2micro.yml ps'"