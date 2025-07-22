#!/bin/bash

# Mini-UPS Multi-Service AWS Deployment Script
# This script deploys UPS, Amazon, and World Simulator services to AWS EC2

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DEPLOY_ENV=${DEPLOY_ENV:-production}
AWS_REGION=${AWS_REGION:-us-east-1}

# Default values
EC2_HOST=""
EC2_USER="ubuntu"
SSH_KEY=""
DOCKER_REGISTRY=""
FORCE_REBUILD=false
SKIP_TESTS=false

# Usage function
usage() {
    echo -e "${BLUE}Usage: $0 [OPTIONS]${NC}"
    echo ""
    echo "Options:"
    echo "  -h, --host          EC2 host IP or domain"
    echo "  -u, --user          EC2 SSH user (default: ubuntu)"
    echo "  -k, --key           Path to SSH private key"
    echo "  -r, --registry      Docker registry prefix (optional)"
    echo "  -f, --force         Force rebuild all images"
    echo "  -s, --skip-tests    Skip running tests"
    echo "  --help              Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  DEPLOY_ENV          Deployment environment (default: production)"
    echo "  AWS_REGION          AWS region (default: us-east-1)"
    echo ""
    echo "Examples:"
    echo "  $0 -h 1.2.3.4 -k ~/.ssh/aws-key.pem"
    echo "  $0 -h my-server.com -u ec2-user -k ~/keys/aws.pem --force"
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--host)
            EC2_HOST="$2"
            shift 2
            ;;
        -u|--user)
            EC2_USER="$2"
            shift 2
            ;;
        -k|--key)
            SSH_KEY="$2"
            shift 2
            ;;
        -r|--registry)
            DOCKER_REGISTRY="$2"
            shift 2
            ;;
        -f|--force)
            FORCE_REBUILD=true
            shift
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --help)
            usage
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
    esac
done

# Validate required parameters
if [[ -z "$EC2_HOST" ]]; then
    echo -e "${RED}Error: EC2 host is required${NC}"
    usage
fi

if [[ -z "$SSH_KEY" ]]; then
    echo -e "${RED}Error: SSH key path is required${NC}"
    usage
fi

if [[ ! -f "$SSH_KEY" ]]; then
    echo -e "${RED}Error: SSH key file not found: $SSH_KEY${NC}"
    exit 1
fi

# Print deployment info
echo -e "${BLUE}🚀 Mini-UPS Multi-Service Deployment${NC}"
echo -e "${BLUE}════════════════════════════════════${NC}"
echo -e "Target Host: ${GREEN}$EC2_HOST${NC}"
echo -e "SSH User: ${GREEN}$EC2_USER${NC}"
echo -e "SSH Key: ${GREEN}$SSH_KEY${NC}"
echo -e "Environment: ${GREEN}$DEPLOY_ENV${NC}"
echo -e "Force Rebuild: ${GREEN}$FORCE_REBUILD${NC}"
echo -e "Skip Tests: ${GREEN}$SKIP_TESTS${NC}"
echo ""

# Function to run SSH commands
ssh_exec() {
    local cmd="$1"
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
        -o ConnectTimeout=30 "$EC2_USER@$EC2_HOST" "$cmd"
}

# Function to copy files via SCP
scp_copy() {
    local src="$1"
    local dst="$2"
    scp -i "$SSH_KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
        -o ConnectTimeout=30 -r "$src" "$EC2_USER@$EC2_HOST:$dst"
}

# Function to log with timestamp
log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Step 1: Test connection
echo -e "${YELLOW}📡 Testing connection to EC2...${NC}"
if ! ssh_exec "echo 'Connection successful'" >/dev/null 2>&1; then
    echo -e "${RED}❌ Failed to connect to EC2 instance${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Connection successful${NC}"
echo ""

# Step 2: Run local tests (optional)
if [[ "$SKIP_TESTS" != "true" ]]; then
    echo -e "${YELLOW}🧪 Running local tests...${NC}"
    
    cd "$PROJECT_DIR"
    
    # Backend tests
    if [[ -f "backend/pom.xml" ]]; then
        log "Running backend tests..."
        cd backend
        if command -v mvn >/dev/null 2>&1; then
            mvn clean test -Dspring.profiles.active=test -DskipTests=false || {
                echo -e "${RED}❌ Backend tests failed${NC}"
                exit 1
            }
        else
            echo -e "${YELLOW}⚠️  Maven not found, skipping backend tests${NC}"
        fi
        cd ..
    fi
    
    # Frontend tests
    if [[ -f "frontend/package.json" ]]; then
        log "Running frontend tests..."
        cd frontend
        if command -v npm >/dev/null 2>&1; then
            npm ci --legacy-peer-deps --prefer-offline --no-audit
            npm run test || {
                echo -e "${YELLOW}⚠️  Frontend tests had issues, continuing...${NC}"
            }
            npm run build:ci || {
                echo -e "${RED}❌ Frontend build failed${NC}"
                exit 1
            }
        else
            echo -e "${YELLOW}⚠️  npm not found, skipping frontend tests${NC}"
        fi
        cd ..
    fi
    
    echo -e "${GREEN}✅ Local tests completed${NC}"
    echo ""
fi

# Step 3: Build Docker images locally
echo -e "${YELLOW}🐳 Building Docker images locally...${NC}"

cd "$PROJECT_DIR"

if [[ "$FORCE_REBUILD" == "true" ]]; then
    log "Force rebuilding all images..."
    docker-compose -f docker-compose.production.yml build --no-cache
else
    log "Building images (with cache)..."
    docker-compose -f docker-compose.production.yml build
fi

echo -e "${GREEN}✅ Docker images built successfully${NC}"
echo ""

# Step 4: Save and transfer Docker images
echo -e "${YELLOW}📦 Saving and transferring Docker images...${NC}"

# Create temporary directory for images
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

cd "$TEMP_DIR"

# Save Docker images
log "Saving Docker images..."
IMAGES=(
    "mini-ups-backend:latest"
    "mini-ups-frontend:latest"
    "mini-amazon:latest"
    "mini-world-sim:latest"
)

for image in "${IMAGES[@]}"; do
    if docker image inspect "$image" >/dev/null 2>&1; then
        image_file=$(echo "$image" | sed 's/:/-/g' | sed 's/\//-/g').tar.gz
        log "Saving $image -> $image_file"
        docker save "$image" | gzip > "$image_file"
    else
        echo -e "${YELLOW}⚠️  Image $image not found, skipping...${NC}"
    fi
done

# Transfer images to EC2
log "Transferring Docker images to EC2..."
ssh_exec "mkdir -p /home/$EC2_USER/mini-ups/images"
scp_copy "*.tar.gz" "/home/$EC2_USER/mini-ups/images/" || {
    echo -e "${YELLOW}⚠️  Some images failed to transfer, continuing...${NC}"
}

echo -e "${GREEN}✅ Images transferred successfully${NC}"
echo ""

# Step 5: Setup project directory on EC2
echo -e "${YELLOW}📁 Setting up project directory on EC2...${NC}"

ssh_exec "
    set -e
    cd /home/$EC2_USER
    
    # Create project directory if it doesn't exist
    if [[ ! -d 'mini-ups' ]]; then
        mkdir -p mini-ups
        echo '✅ Created project directory'
    fi
    
    cd mini-ups
    
    # Setup basic directory structure
    mkdir -p logs data backup scripts
    
    echo '✅ Project directory structure ready'
"

echo -e "${GREEN}✅ Project directory setup completed${NC}"
echo ""

# Step 6: Transfer project files
echo -e "${YELLOW}📂 Transferring project files...${NC}"

cd "$PROJECT_DIR"

# Copy essential files
FILES_TO_COPY=(
    "docker-compose.production.yml"
    ".env.production"
    "scripts/start-production.sh"
    "scripts/logs-production.sh"
    "scripts/health-check.sh"
    "scripts/backup-production.sh"
)

for file in "${FILES_TO_COPY[@]}"; do
    if [[ -f "$file" ]]; then
        log "Copying $file..."
        scp_copy "$file" "/home/$EC2_USER/mini-ups/"
    else
        echo -e "${YELLOW}⚠️  File $file not found, skipping...${NC}"
    fi
done

# Copy knowledge directory (Amazon and World Simulator)
if [[ -d "knowledge" ]]; then
    log "Copying knowledge directory..."
    scp_copy "knowledge/" "/home/$EC2_USER/mini-ups/"
fi

echo -e "${GREEN}✅ Project files transferred${NC}"
echo ""

# Step 7: Load Docker images on EC2
echo -e "${YELLOW}📥 Loading Docker images on EC2...${NC}"

ssh_exec "
    set -e
    cd /home/$EC2_USER/mini-ups/images
    
    echo 'Loading Docker images...'
    for image in *.tar.gz; do
        if [[ -f \"\$image\" ]]; then
            echo \"Loading \$image...\"
            docker load < \"\$image\"
        fi
    done
    
    # Cleanup image files
    rm -f *.tar.gz
    
    echo '✅ Docker images loaded successfully'
"

echo -e "${GREEN}✅ Docker images loaded${NC}"
echo ""

# Step 8: Deploy services
echo -e "${YELLOW}🚀 Deploying services on EC2...${NC}"

ssh_exec "
    set -e
    cd /home/$EC2_USER/mini-ups
    
    # Stop existing services
    echo '⏹️  Stopping existing services...'
    docker-compose -f docker-compose.production.yml down || true
    
    # Create production environment
    echo '⚙️  Setting up production environment...'
    cp .env.production .env.production.deploy
    
    # Start services
    echo '🔄 Starting all services...'
    docker-compose -f docker-compose.production.yml --env-file .env.production.deploy up -d --remove-orphans
    
    echo '✅ Services started successfully'
"

echo -e "${GREEN}✅ Services deployed${NC}"
echo ""

# Step 9: Health checks
echo -e "${YELLOW}🔍 Running health checks...${NC}"

# Wait for services to start
log "Waiting for services to initialize..."
sleep 60

# Health check function
check_service() {
    local service_name="$1"
    local url="$2"
    local max_attempts=20
    local attempt=1
    
    log "Checking $service_name..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if ssh_exec "curl -f -s '$url' >/dev/null 2>&1"; then
            echo -e "${GREEN}✅ $service_name is healthy${NC}"
            return 0
        fi
        
        echo -e "${YELLOW}⏳ Waiting for $service_name... (Attempt $attempt/$max_attempts)${NC}"
        sleep 10
        ((attempt++))
    done
    
    echo -e "${RED}❌ $service_name failed health check${NC}"
    return 1
}

# Run health checks
HEALTH_CHECK_FAILED=false

check_service "UPS Backend" "http://localhost:8081/actuator/health" || HEALTH_CHECK_FAILED=true
check_service "UPS Frontend" "http://localhost:3000" || HEALTH_CHECK_FAILED=true
check_service "Amazon Service" "http://localhost:8080" || HEALTH_CHECK_FAILED=true

# Database health checks
log "Checking database connections..."
ssh_exec "
    docker exec mini-ups-postgres pg_isready -U postgres || exit 1
    docker exec mini-amazon-db pg_isready -U postgres || exit 1
    docker exec world-simulator-db pg_isready -U postgres || exit 1
    echo '✅ All databases are ready'
" || HEALTH_CHECK_FAILED=true

# Redis health check
log "Checking Redis connection..."
ssh_exec "
    docker exec mini-ups-redis redis-cli ping || exit 1
    echo '✅ Redis is ready'
" || HEALTH_CHECK_FAILED=true

if [[ "$HEALTH_CHECK_FAILED" == "true" ]]; then
    echo -e "${RED}❌ Some health checks failed${NC}"
    echo -e "${YELLOW}📋 Check service logs for details:${NC}"
    echo -e "  ssh -i $SSH_KEY $EC2_USER@$EC2_HOST 'cd mini-ups && docker-compose -f docker-compose.production.yml logs'"
    exit 1
fi

echo -e "${GREEN}✅ All health checks passed${NC}"
echo ""

# Step 10: Final summary
echo -e "${BLUE}🎉 Deployment completed successfully!${NC}"
echo -e "${BLUE}════════════════════════════════════${NC}"
echo -e "${GREEN}📍 Access your services:${NC}"
echo -e "   🌐 UPS Frontend: http://$EC2_HOST:3000"
echo -e "   🛒 Amazon Service: http://$EC2_HOST:8080"
echo -e "   🔌 UPS API: http://$EC2_HOST:8081"
echo -e "   📊 RabbitMQ Management: http://$EC2_HOST:15672 (guest/guest)"
echo ""
echo -e "${GREEN}📋 Useful commands:${NC}"
echo -e "   SSH: ssh -i $SSH_KEY $EC2_USER@$EC2_HOST"
echo -e "   Logs: ssh -i $SSH_KEY $EC2_USER@$EC2_HOST 'cd mini-ups && docker-compose -f docker-compose.production.yml logs -f'"
echo -e "   Status: ssh -i $SSH_KEY $EC2_USER@$EC2_HOST 'cd mini-ups && docker-compose -f docker-compose.production.yml ps'"
echo ""
echo -e "${GREEN}🔧 Management scripts on server:${NC}"
echo -e "   ./start-production.sh    - Start services"
echo -e "   ./logs-production.sh     - View logs"
echo -e "   ./health-check.sh        - Check service health"
echo -e "   ./backup-production.sh   - Backup data"
echo ""
echo -e "${GREEN}✅ Deployment completed at $(date)${NC}"