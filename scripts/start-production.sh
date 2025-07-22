#!/bin/bash

# Mini-UPS Production Startup Script
# Starts all services (UPS, Amazon, World Simulator) in production mode

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="docker-compose.production.yml"
ENV_FILE=".env.production"

# Function to log with timestamp
log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
        exit 1
    fi
}

# Function to check if Docker Compose is available
check_docker_compose() {
    if ! docker-compose --version >/dev/null 2>&1; then
        echo -e "${RED}❌ Docker Compose is not available. Please install Docker Compose.${NC}"
        exit 1
    fi
}

# Function to create necessary directories
create_directories() {
    log "Creating necessary directories..."
    mkdir -p logs data backup
    
    # Create log directories with proper permissions
    if [[ "$EUID" -eq 0 ]]; then
        # Running as root
        mkdir -p /var/log/mini-ups
        chown -R 1000:1000 /var/log/mini-ups 2>/dev/null || true
    else
        # Running as non-root user
        sudo mkdir -p /var/log/mini-ups 2>/dev/null || mkdir -p "$HOME/.local/log/mini-ups"
        sudo chown -R $(id -u):$(id -g) /var/log/mini-ups 2>/dev/null || true
    fi
    
    echo -e "${GREEN}✅ Directories created${NC}"
}

# Function to validate environment file
validate_env_file() {
    if [[ ! -f "$ENV_FILE" ]]; then
        echo -e "${RED}❌ Environment file not found: $ENV_FILE${NC}"
        echo -e "${YELLOW}Creating default environment file...${NC}"
        
        cat > "$ENV_FILE" <<EOF
# Mini-UPS Production Environment
UPS_POSTGRES_DB=ups_db
UPS_POSTGRES_USER=postgres
UPS_POSTGRES_PASSWORD=abc123
UPS_REDIS_PASSWORD=test123
UPS_RABBITMQ_USER=guest
UPS_RABBITMQ_PASSWORD=guest
JWT_SECRET=change-this-in-production-min-256-bits-secure
NUM_TRUCKS=5
WORLD_ID=1
AMAZON_SECRET_KEY=change-this-amazon-secret-key
UPS_BACKEND_IMAGE=mini-ups-backend:latest
UPS_FRONTEND_IMAGE=mini-ups-frontend:latest
EOF
        
        echo -e "${YELLOW}⚠️  Default environment file created. Please update the secrets!${NC}"
    fi
    
    # Check for default/insecure secrets
    if grep -q "change-this" "$ENV_FILE" 2>/dev/null; then
        echo -e "${YELLOW}⚠️  Warning: Default secrets detected in $ENV_FILE${NC}"
        echo -e "${YELLOW}   Please update JWT_SECRET and AMAZON_SECRET_KEY for production use${NC}"
    fi
}

# Function to stop existing services
stop_existing_services() {
    log "Stopping any existing services..."
    
    if docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps -q | grep -q .; then
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
        echo -e "${GREEN}✅ Existing services stopped${NC}"
    else
        echo -e "${BLUE}ℹ️  No running services found${NC}"
    fi
}

# Function to pull latest images (if using registry)
pull_images() {
    log "Pulling latest Docker images..."
    
    # Only pull if images are from a registry (contain '/')
    if grep -E "(BACKEND_IMAGE|FRONTEND_IMAGE).*/" "$ENV_FILE" >/dev/null 2>&1; then
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull || {
            echo -e "${YELLOW}⚠️  Failed to pull some images, continuing with local images...${NC}"
        }
    else
        echo -e "${BLUE}ℹ️  Using local images${NC}"
    fi
}

# Function to start services
start_services() {
    log "Starting all services..."
    
    # Start services in detached mode
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --remove-orphans
    
    echo -e "${GREEN}✅ Services started${NC}"
}

# Function to wait for services to be ready
wait_for_services() {
    log "Waiting for services to be ready..."
    
    local max_attempts=30
    local attempt=1
    
    # Wait a bit for services to start
    sleep 20
    
    # Check UPS Backend health
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s http://localhost:8081/actuator/health >/dev/null 2>&1; then
            echo -e "${GREEN}✅ UPS Backend is ready${NC}"
            break
        fi
        
        echo -e "${YELLOW}⏳ Waiting for UPS Backend... (Attempt $attempt/$max_attempts)${NC}"
        sleep 10
        ((attempt++))
        
        if [[ $attempt -gt $max_attempts ]]; then
            echo -e "${RED}❌ UPS Backend failed to start in time${NC}"
            return 1
        fi
    done
    
    # Quick checks for other services
    sleep 5
    
    # Check UPS Frontend
    if curl -f -s http://localhost:3000 >/dev/null 2>&1; then
        echo -e "${GREEN}✅ UPS Frontend is ready${NC}"
    else
        echo -e "${YELLOW}⚠️  UPS Frontend not responding yet${NC}"
    fi
    
    # Check Amazon Service
    if curl -f -s http://localhost:8080 >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Amazon Service is ready${NC}"
    else
        echo -e "${YELLOW}⚠️  Amazon Service not responding yet${NC}"
    fi
    
    # Check databases
    if docker exec mini-ups-postgres pg_isready -U postgres >/dev/null 2>&1; then
        echo -e "${GREEN}✅ UPS Database is ready${NC}"
    else
        echo -e "${YELLOW}⚠️  UPS Database not ready${NC}"
    fi
    
    if docker exec mini-amazon-db pg_isready -U postgres >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Amazon Database is ready${NC}"
    else
        echo -e "${YELLOW}⚠️  Amazon Database not ready${NC}"
    fi
    
    if docker exec world-simulator-db pg_isready -U postgres >/dev/null 2>&1; then
        echo -e "${GREEN}✅ World Simulator Database is ready${NC}"
    else
        echo -e "${YELLOW}⚠️  World Simulator Database not ready${NC}"
    fi
    
    # Check Redis
    if docker exec mini-ups-redis redis-cli ping >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Redis is ready${NC}"
    else
        echo -e "${YELLOW}⚠️  Redis not ready${NC}"
    fi
}

# Function to show service status
show_status() {
    echo ""
    log "Service Status:"
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
    echo ""
}

# Function to show access URLs
show_access_info() {
    local host_ip="localhost"
    
    # Try to get public IP if possible
    if command -v curl >/dev/null 2>&1; then
        public_ip=$(curl -s -m 5 http://ifconfig.me 2>/dev/null || echo "")
        if [[ -n "$public_ip" && "$public_ip" != *"error"* ]]; then
            host_ip="$public_ip"
        fi
    fi
    
    echo -e "${BLUE}🌐 Access URLs:${NC}"
    echo -e "   UPS Frontend:        http://$host_ip:3000"
    echo -e "   Amazon Service:      http://$host_ip:8080"
    echo -e "   UPS API:            http://$host_ip:8081"
    echo -e "   UPS API Health:     http://$host_ip:8081/actuator/health"
    echo -e "   RabbitMQ Management: http://$host_ip:15672 (guest/guest)"
    echo ""
    echo -e "${BLUE}📊 Database Access:${NC}"
    echo -e "   UPS Database:        localhost:5431 (ups_db/postgres/abc123)"
    echo -e "   Amazon Database:     localhost:15432 (mini_amazon/postgres/abc123)"
    echo -e "   World Database:      localhost:5433 (worldSim/postgres/abc123)"
    echo -e "   Redis Cache:         localhost:6380"
    echo ""
}

# Function to show management commands
show_management_info() {
    echo -e "${BLUE}🔧 Management Commands:${NC}"
    echo -e "   View logs:           ./logs-production.sh"
    echo -e "   Health check:        ./health-check.sh"
    echo -e "   Backup data:         ./backup-production.sh"
    echo -e "   Stop services:       docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE down"
    echo -e "   Restart service:     docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE restart <service>"
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}🚀 Starting Mini-UPS Production Services${NC}"
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    
    cd "$PROJECT_DIR"
    
    # Pre-flight checks
    check_docker
    check_docker_compose
    
    # Setup
    create_directories
    validate_env_file
    
    # Stop existing services
    stop_existing_services
    
    # Pull images (if needed)
    pull_images
    
    # Start services
    start_services
    
    # Wait for services to be ready
    if wait_for_services; then
        echo -e "${GREEN}✅ All services started successfully${NC}"
    else
        echo -e "${YELLOW}⚠️  Some services may not be fully ready yet${NC}"
        echo -e "${YELLOW}   Use ./health-check.sh to check status later${NC}"
    fi
    
    # Show status and access information
    show_status
    show_access_info
    show_management_info
    
    echo -e "${GREEN}🎉 Mini-UPS is now running!${NC}"
    echo -e "${BLUE}Use 'docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE logs -f' to follow logs${NC}"
}

# Handle script arguments
case "${1:-}" in
    --help|-h)
        echo "Mini-UPS Production Startup Script"
        echo ""
        echo "Usage: $0 [OPTIONS]"
        echo ""
        echo "Options:"
        echo "  --help, -h    Show this help message"
        echo ""
        echo "This script will:"
        echo "  1. Check Docker and Docker Compose availability"
        echo "  2. Create necessary directories"
        echo "  3. Validate environment configuration"
        echo "  4. Stop any existing services"
        echo "  5. Pull latest images (if using registry)"
        echo "  6. Start all services (UPS, Amazon, World Simulator)"
        echo "  7. Wait for services to be ready"
        echo "  8. Display access information"
        echo ""
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac