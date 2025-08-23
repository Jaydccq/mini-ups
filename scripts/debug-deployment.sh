#!/bin/bash

# Debug deployment script for AWS EC2 Mini-UPS
# This script helps diagnose common deployment issues

set -e

echo "🔍 Mini-UPS Deployment Diagnostics"
echo "=================================="

# Function to check command availability
check_command() {
    if command -v "$1" &> /dev/null; then
        echo "✅ $1 is available"
    else
        echo "❌ $1 is not available"
    fi
}

# Function to check port accessibility
check_port() {
    local host=$1
    local port=$2
    local service=$3
    
    if timeout 5 bash -c "</dev/tcp/$host/$port"; then
        echo "✅ $service ($host:$port) is accessible"
    else
        echo "❌ $service ($host:$port) is not accessible"
    fi
}

# System Information
echo ""
echo "📋 System Information:"
echo "OS: $(uname -a)"
echo "User: $(whoami)"
echo "Working Directory: $(pwd)"
echo "Disk Usage:"
df -h /
echo ""

# Check essential commands
echo "🔧 Checking Essential Commands:"
check_command docker
check_command docker-compose
check_command curl
check_command git
check_command pg_isready

# Docker status
echo ""
echo "🐳 Docker Status:"
if systemctl is-active --quiet docker; then
    echo "✅ Docker service is running"
else
    echo "❌ Docker service is not running"
    echo "Starting Docker service..."
    sudo systemctl start docker
fi

echo "Docker version:"
docker --version
echo "Docker Compose version:"
docker compose version

# Check running containers
echo ""
echo "📦 Running Containers:"
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

# Check Docker images
echo ""
echo "💾 Available Docker Images:"
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"

# Network connectivity
echo ""
echo "🌐 Network Connectivity:"
check_port "localhost" "22" "SSH"
check_port "localhost" "5432" "PostgreSQL"
check_port "localhost" "6380" "Redis"
check_port "localhost" "8081" "UPS Backend"
check_port "localhost" "3000" "UPS Frontend"

# GitHub Container Registry connectivity
echo ""
echo "📡 GitHub Container Registry Connectivity:"
if timeout 10 docker pull hello-world &> /dev/null; then
    echo "✅ Can pull from Docker Hub"
    docker rmi hello-world &> /dev/null
else
    echo "❌ Cannot pull from Docker Hub"
fi

# Check if logged into GitHub Container Registry
echo ""
echo "🔐 Container Registry Authentication:"
if docker info | grep -i "registry" &> /dev/null; then
    echo "✅ Docker is configured with registries"
else
    echo "⚠️  No registry authentication detected"
fi

# Environment files check
echo ""
echo "📁 Environment Files:"
for env_file in ".env" ".env.staging" ".env.production" ".env.local"; do
    if [[ -f "$env_file" ]]; then
        echo "✅ $env_file exists ($(wc -l < $env_file) lines)"
    else
        echo "❌ $env_file missing"
    fi
done

# Docker compose files check
echo ""
echo "📋 Docker Compose Files:"
for compose_file in "docker-compose.yml" "docker-compose.t2micro.yml" "docker-compose.production.yml"; do
    if [[ -f "$compose_file" ]]; then
        echo "✅ $compose_file exists"
    else
        echo "❌ $compose_file missing"
    fi
done

# Git status
echo ""
echo "📚 Git Status:"
if [[ -d ".git" ]]; then
    echo "✅ Git repository detected"
    echo "Current branch: $(git branch --show-current)"
    echo "Last commit: $(git log -1 --pretty=format:'%h %s')"
    echo "Remote origin: $(git remote get-url origin 2>/dev/null || echo 'Not set')"
else
    echo "❌ Not a git repository"
fi

# Memory and disk usage
echo ""
echo "💾 Resource Usage:"
echo "Memory usage:"
free -h
echo ""
echo "Disk usage:"
df -h
echo ""
echo "Docker system usage:"
docker system df

# Service health checks
echo ""
echo "🏥 Service Health Checks:"

# Check database if container is running
if docker ps --format '{{.Names}}' | grep -q postgres; then
    db_container=$(docker ps --format '{{.Names}}' | grep postgres | head -1)
    if docker exec "$db_container" pg_isready -U postgres &> /dev/null; then
        echo "✅ PostgreSQL is healthy in container: $db_container"
    else
        echo "❌ PostgreSQL is not healthy in container: $db_container"
    fi
else
    echo "⚠️  No PostgreSQL container running"
fi

# Check Redis if container is running
if docker ps --format '{{.Names}}' | grep -q redis; then
    redis_container=$(docker ps --format '{{.Names}}' | grep redis | head -1)
    if docker exec "$redis_container" redis-cli ping &> /dev/null; then
        echo "✅ Redis is healthy in container: $redis_container"
    else
        echo "❌ Redis is not healthy in container: $redis_container"
    fi
else
    echo "⚠️  No Redis container running"
fi

# Check backend health
echo ""
echo "🔍 Application Health Checks:"
if curl -f -s http://localhost:8081/actuator/health &> /dev/null; then
    echo "✅ UPS Backend is healthy"
    curl -s http://localhost:8081/actuator/health | python3 -m json.tool 2>/dev/null || echo "Health response received"
else
    echo "❌ UPS Backend is not healthy"
fi

if curl -f -s http://localhost:3000 &> /dev/null; then
    echo "✅ UPS Frontend is accessible"
else
    echo "❌ UPS Frontend is not accessible"
fi

# Recent logs if containers are running
echo ""
echo "📋 Recent Container Logs (last 10 lines):"
for container in $(docker ps --format '{{.Names}}'); do
    echo ""
    echo "--- $container logs ---"
    docker logs "$container" --tail 10 2>/dev/null || echo "No logs available"
done

# Recommendations
echo ""
echo "💡 Troubleshooting Recommendations:"
echo ""

# Check if containers are not running
if ! docker ps --format '{{.Names}}' | grep -q mini-ups; then
    echo "❗ No Mini-UPS containers are running. Try:"
    echo "   docker compose -f docker-compose.t2micro.yml up -d"
fi

# Check for common issues
if ! systemctl is-active --quiet docker; then
    echo "❗ Docker service is not running. Start it with:"
    echo "   sudo systemctl start docker"
    echo "   sudo systemctl enable docker"
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❗ Docker Compose is not available. Install it or use 'docker compose' instead."
fi

# Check disk space
disk_usage=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
if [[ $disk_usage -gt 80 ]]; then
    echo "❗ Disk usage is high ($disk_usage%). Clean up with:"
    echo "   docker system prune -f"
fi

# Check memory usage
memory_usage=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}')
if [[ $memory_usage -gt 90 ]]; then
    echo "❗ Memory usage is high ($memory_usage%). Consider:"
    echo "   - Reducing container memory limits"
    echo "   - Using t2.micro optimized configuration"
fi

echo ""
echo "🎯 Quick Fixes:"
echo "1. Restart all services: docker compose -f docker-compose.t2micro.yml down && docker compose -f docker-compose.t2micro.yml up -d"
echo "2. Clean up Docker: docker system prune -f"
echo "3. Check logs: docker compose -f docker-compose.t2micro.yml logs -f"
echo "4. Pull latest images: docker compose -f docker-compose.t2micro.yml pull"
echo ""
echo "✨ Diagnostics complete!"