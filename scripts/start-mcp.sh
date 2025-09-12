#!/bin/bash

# Mini-UPS MCP Server Startup Script
# Starts the MCP server for local development

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[MCP-SERVER]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[MCP-SERVER]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[MCP-SERVER]${NC} $1"
}

print_error() {
    echo -e "${RED}[MCP-SERVER]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "docker-compose.yml" ]; then
    print_error "Error: docker-compose.yml not found. Please run this script from the project root directory."
    exit 1
fi

if [ ! -d "mcp-server" ]; then
    print_error "Error: mcp-server directory not found. Please ensure the MCP server is set up."
    exit 1
fi

print_status "Starting Mini-UPS MCP Server..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Create projectnet network if it doesn't exist
if ! docker network inspect projectnet > /dev/null 2>&1; then
    print_status "Creating projectnet network..."
    docker network create projectnet
fi

# Check for .env file and create if needed
if [ ! -f ".env" ]; then
    print_warning "No .env file found. Creating default .env file..."
    cat > .env << EOF
# Mini-UPS Environment Configuration

# Database Configuration
POSTGRES_DB=ups_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=abc123

# Redis Configuration
REDIS_PASSWORD=test123

# RabbitMQ Configuration
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# JWT Configuration (REQUIRED)
JWT_SECRET=mini-ups-development-jwt-secret-key-32-chars-minimum-length-for-security

# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=docker

# MCP Server Configuration
MCP_AUTH_TOKEN=
MCP_API_KEY=
MCP_LOG_LEVEL=info
MCP_WAIT_FOR_BACKEND=true

# Optional: Number of trucks for world simulator
NUM_TRUCKS=5

# Optional: World ID for world simulator
WORLD_ID=
EOF
    print_warning "Please update the .env file with appropriate values before running in production."
fi

# Parse command line arguments
MODE="full"
DETACHED=""
BUILD=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --mcp-only)
            MODE="mcp-only"
            shift
            ;;
        --detached|-d)
            DETACHED="-d"
            shift
            ;;
        --build)
            BUILD="--build"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --mcp-only     Start only the MCP server (requires backend to be running)"
            echo "  --detached,-d  Run in detached mode"
            echo "  --build        Force rebuild of images"
            echo "  --help,-h      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Start full system including MCP server"
            echo "  $0 --mcp-only        # Start only MCP server"
            echo "  $0 --detached        # Start in background"
            echo "  $0 --build           # Rebuild and start"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information."
            exit 1
            ;;
    esac
done

# Check if backend is running when using --mcp-only
if [ "$MODE" = "mcp-only" ]; then
    if ! docker ps --format 'table {{.Names}}' | grep -q "mini-ups-backend"; then
        print_error "Backend is not running. Please start the backend first or use full mode."
        print_status "To start the backend: docker compose up ups-backend -d"
        exit 1
    fi
    
    print_status "Starting MCP server only..."
    docker compose up $DETACHED $BUILD mcp-server
else
    print_status "Starting full Mini-UPS system with MCP server..."
    
    # Start services in dependency order for better startup experience
    print_status "Starting databases..."
    docker compose up $DETACHED $BUILD ups-database redis rabbitmq
    
    if [ -z "$DETACHED" ]; then
        print_status "Waiting for databases to be ready..."
        sleep 5
    fi
    
    print_status "Starting world simulator and Amazon services..."
    docker compose up $DETACHED $BUILD worldsim-db amazon-db world-simulator amazon-web
    
    if [ -z "$DETACHED" ]; then
        print_status "Waiting for external services to be ready..."
        sleep 5
    fi
    
    print_status "Starting UPS backend..."
    docker compose up $DETACHED $BUILD ups-backend
    
    if [ -z "$DETACHED" ]; then
        print_status "Waiting for backend to be ready..."
        sleep 10
    fi
    
    print_status "Starting frontend and MCP server..."
    docker compose up $DETACHED $BUILD ups-frontend mcp-server
fi

if [ -z "$DETACHED" ]; then
    print_success "Mini-UPS MCP Server startup complete!"
    print_status "Services available at:"
    echo "  - UPS Frontend: http://localhost:3000"
    echo "  - UPS Backend: http://localhost:8081"
    echo "  - Amazon Service: http://localhost:8080"
    echo "  - MCP Server: Running in container (stdio interface)"
    echo ""
    print_status "To use the MCP server:"
    echo "  1. Connect to the container: docker exec -it mini-ups-mcp-server /bin/sh"
    echo "  2. Or use it via MCP client tools"
    echo ""
    print_status "Press Ctrl+C to stop all services"
else
    print_success "Mini-UPS MCP Server started in detached mode!"
    print_status "Check logs with: docker compose logs -f mcp-server"
    print_status "Stop with: docker compose down"
fi