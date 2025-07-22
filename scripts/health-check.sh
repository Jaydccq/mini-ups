#!/bin/bash

# Mini-UPS Production Health Check Script
# Comprehensive health monitoring for all services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="docker-compose.production.yml"
ENV_FILE=".env.production"

# Default values
DETAILED=false
JSON_OUTPUT=false
CONTINUOUS=false
INTERVAL=30

# Usage function
usage() {
    echo -e "${BLUE}Mini-UPS Production Health Check${NC}"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -d, --detailed    Show detailed health information"
    echo "  -j, --json        Output results in JSON format"
    echo "  -c, --continuous  Run continuously (every 30 seconds)"
    echo "  -i, --interval N  Set interval for continuous mode (seconds)"
    echo "  -h, --help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                # Basic health check"
    echo "  $0 -d             # Detailed health check"
    echo "  $0 -c             # Continuous monitoring"
    echo "  $0 -c -i 60       # Continuous monitoring every 60 seconds"
    echo "  $0 -j             # JSON output for automation"
    echo ""
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--detailed)
            DETAILED=true
            shift
            ;;
        -j|--json)
            JSON_OUTPUT=true
            shift
            ;;
        -c|--continuous)
            CONTINUOUS=true
            shift
            ;;
        -i|--interval)
            INTERVAL="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
    esac
done

# Function to log with timestamp
log() {
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    fi
}

# Function to check service container status
check_container_status() {
    local service_name="$1"
    local container_name="$2"
    
    if docker inspect "$container_name" >/dev/null 2>&1; then
        local status=$(docker inspect --format='{{.State.Status}}' "$container_name")
        local health=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "none")
        local uptime=$(docker inspect --format='{{.State.StartedAt}}' "$container_name")
        
        if [[ "$status" == "running" ]]; then
            if [[ "$health" == "healthy" ]]; then
                echo "healthy"
            elif [[ "$health" == "unhealthy" ]]; then
                echo "unhealthy"
            elif [[ "$health" == "starting" ]]; then
                echo "starting"
            else
                echo "running"
            fi
        else
            echo "stopped"
        fi
    else
        echo "missing"
    fi
}

# Function to check HTTP endpoint
check_http_endpoint() {
    local url="$1"
    local timeout="${2:-10}"
    
    if curl -f -s -m "$timeout" "$url" >/dev/null 2>&1; then
        echo "healthy"
    else
        echo "unhealthy"
    fi
}

# Function to check database connection
check_database() {
    local container_name="$1"
    local user="${2:-postgres}"
    
    if docker exec "$container_name" pg_isready -U "$user" >/dev/null 2>&1; then
        echo "healthy"
    else
        echo "unhealthy"
    fi
}

# Function to check Redis connection
check_redis() {
    local container_name="$1"
    local password="${2:-}"
    
    if [[ -n "$password" ]]; then
        if docker exec "$container_name" redis-cli -a "$password" ping >/dev/null 2>&1; then
            echo "healthy"
        else
            echo "unhealthy"
        fi
    else
        if docker exec "$container_name" redis-cli ping >/dev/null 2>&1; then
            echo "healthy"
        else
            echo "unhealthy"
        fi
    fi
}

# Function to get resource usage
get_resource_usage() {
    local container_name="$1"
    
    if docker stats --no-stream --format "table {{.CPUPerc}},{{.MemUsage}},{{.NetIO}},{{.BlockIO}}" "$container_name" 2>/dev/null; then
        return 0
    else
        echo "N/A,N/A,N/A,N/A"
        return 1
    fi
}

# Function to perform health checks
perform_health_checks() {
    local results=()
    local overall_status="healthy"
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        echo -e "${BLUE}🏥 Mini-UPS Health Check Report${NC}"
        echo -e "${BLUE}════════════════════════════════${NC}"
        echo -e "Time: $(date)"
        echo ""
    fi
    
    # UPS Services
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        echo -e "${CYAN}📦 UPS Services${NC}"
        echo "─────────────────────────────────────────"
    fi
    
    # UPS Backend
    local ups_backend_status=$(check_container_status "ups-backend" "mini-ups-backend")
    local ups_backend_http=$(check_http_endpoint "http://localhost:8081/actuator/health")
    
    if [[ "$ups_backend_status" != "healthy" && "$ups_backend_status" != "running" ]] || [[ "$ups_backend_http" != "healthy" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("ups-backend,$ups_backend_status,$ups_backend_http")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "UPS Backend" "$ups_backend_status" "$ups_backend_http"
    fi
    
    # UPS Frontend
    local ups_frontend_status=$(check_container_status "ups-frontend" "mini-ups-frontend")
    local ups_frontend_http=$(check_http_endpoint "http://localhost:3000")
    
    if [[ "$ups_frontend_status" != "healthy" && "$ups_frontend_status" != "running" ]] || [[ "$ups_frontend_http" != "healthy" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("ups-frontend,$ups_frontend_status,$ups_frontend_http")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "UPS Frontend" "$ups_frontend_status" "$ups_frontend_http"
    fi
    
    # UPS Database
    local ups_db_status=$(check_container_status "ups-database" "mini-ups-postgres")
    local ups_db_conn=$(check_database "mini-ups-postgres")
    
    if [[ "$ups_db_status" != "healthy" && "$ups_db_status" != "running" ]] || [[ "$ups_db_conn" != "healthy" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("ups-database,$ups_db_status,$ups_db_conn")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "UPS Database" "$ups_db_status" "$ups_db_conn"
    fi
    
    # UPS Redis
    local ups_redis_status=$(check_container_status "ups-redis" "mini-ups-redis")
    local ups_redis_conn=$(check_redis "mini-ups-redis" "test123")
    
    if [[ "$ups_redis_status" != "healthy" && "$ups_redis_status" != "running" ]] || [[ "$ups_redis_conn" != "healthy" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("ups-redis,$ups_redis_status,$ups_redis_conn")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "UPS Redis" "$ups_redis_status" "$ups_redis_conn"
    fi
    
    # UPS RabbitMQ
    local ups_rabbitmq_status=$(check_container_status "ups-rabbitmq" "mini-ups-rabbitmq")
    local ups_rabbitmq_http=$(check_http_endpoint "http://localhost:15672")
    
    if [[ "$ups_rabbitmq_status" != "healthy" && "$ups_rabbitmq_status" != "running" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("ups-rabbitmq,$ups_rabbitmq_status,$ups_rabbitmq_http")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "UPS RabbitMQ" "$ups_rabbitmq_status" "$ups_rabbitmq_http"
        echo ""
    fi
    
    # Amazon Services
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        echo -e "${CYAN}🛒 Amazon Services${NC}"
        echo "─────────────────────────────────────────"
    fi
    
    # Amazon Web
    local amazon_web_status=$(check_container_status "amazon-web" "mini-amazon-web")
    local amazon_web_http=$(check_http_endpoint "http://localhost:8080")
    
    if [[ "$amazon_web_status" != "healthy" && "$amazon_web_status" != "running" ]] || [[ "$amazon_web_http" != "healthy" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("amazon-web,$amazon_web_status,$amazon_web_http")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "Amazon Web" "$amazon_web_status" "$amazon_web_http"
    fi
    
    # Amazon Database
    local amazon_db_status=$(check_container_status "amazon-db" "mini-amazon-db")
    local amazon_db_conn=$(check_database "mini-amazon-db")
    
    if [[ "$amazon_db_status" != "healthy" && "$amazon_db_status" != "running" ]] || [[ "$amazon_db_conn" != "healthy" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("amazon-db,$amazon_db_status,$amazon_db_conn")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "Amazon Database" "$amazon_db_status" "$amazon_db_conn"
        echo ""
    fi
    
    # World Simulator Services
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        echo -e "${CYAN}🌍 World Simulator Services${NC}"
        echo "─────────────────────────────────────────"
    fi
    
    # World Server
    local world_server_status=$(check_container_status "world-server" "world-simulator-server")
    local world_server_tcp="unknown"
    
    # Check TCP ports (simplified check)
    if netstat -ln 2>/dev/null | grep -q ":12345.*LISTEN" && netstat -ln 2>/dev/null | grep -q ":23456.*LISTEN"; then
        world_server_tcp="healthy"
    elif [[ "$world_server_status" == "running" ]]; then
        world_server_tcp="starting"
    else
        world_server_tcp="unhealthy"
    fi
    
    if [[ "$world_server_status" != "healthy" && "$world_server_status" != "running" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("world-server,$world_server_status,$world_server_tcp")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "World Server" "$world_server_status" "$world_server_tcp"
    fi
    
    # World Database
    local world_db_status=$(check_container_status "world-db" "world-simulator-db")
    local world_db_conn=$(check_database "world-simulator-db")
    
    if [[ "$world_db_status" != "healthy" && "$world_db_status" != "running" ]] || [[ "$world_db_conn" != "healthy" ]]; then
        overall_status="unhealthy"
    fi
    
    results+=("world-db,$world_db_status,$world_db_conn")
    
    if [[ "$JSON_OUTPUT" != "true" ]]; then
        printf "%-20s %-12s %-12s\n" "World Database" "$world_db_status" "$world_db_conn"
        echo ""
    fi
    
    # Show detailed information if requested
    if [[ "$DETAILED" == "true" && "$JSON_OUTPUT" != "true" ]]; then
        echo -e "${CYAN}📊 Resource Usage${NC}"
        echo "─────────────────────────────────────────"
        echo -e "Service              CPU%     Memory       Network I/O      Block I/O"
        
        for service in mini-ups-backend mini-ups-frontend mini-ups-postgres mini-ups-redis mini-ups-rabbitmq mini-amazon-web mini-amazon-db world-simulator-server world-simulator-db; do
            if docker inspect "$service" >/dev/null 2>&1; then
                local stats=$(get_resource_usage "$service")
                printf "%-20s %s\n" "$service" "$stats" | tr ',' ' '
            fi
        done
        echo ""
        
        # System information
        echo -e "${CYAN}💻 System Information${NC}"
        echo "─────────────────────────────────────────"
        echo "Docker version: $(docker --version 2>/dev/null || echo 'N/A')"
        echo "Docker Compose: $(docker-compose --version 2>/dev/null || echo 'N/A')"
        echo "Disk usage: $(df -h / 2>/dev/null | tail -1 | awk '{print $5 " used"}' || echo 'N/A')"
        echo "Memory usage: $(free -h 2>/dev/null | grep Mem | awk '{print $3 "/" $2}' || echo 'N/A')"
        echo "Load average: $(uptime 2>/dev/null | awk -F'load average:' '{print $2}' | xargs || echo 'N/A')"
        echo ""
    fi
    
    # Overall status
    if [[ "$JSON_OUTPUT" == "true" ]]; then
        # JSON output
        echo "{"
        echo "  \"timestamp\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\","
        echo "  \"overall_status\": \"$overall_status\","
        echo "  \"services\": ["
        
        local first=true
        for result in "${results[@]}"; do
            IFS=',' read -r service container_status endpoint_status <<< "$result"
            
            if [[ "$first" == "true" ]]; then
                first=false
            else
                echo ","
            fi
            
            echo -n "    {"
            echo -n "\"service\": \"$service\", "
            echo -n "\"container_status\": \"$container_status\", "
            echo -n "\"endpoint_status\": \"$endpoint_status\""
            echo -n "}"
        done
        
        echo ""
        echo "  ]"
        echo "}"
    else
        # Summary
        echo -e "${BLUE}📋 Health Summary${NC}"
        echo "─────────────────────────────────────────"
        
        if [[ "$overall_status" == "healthy" ]]; then
            echo -e "Overall Status: ${GREEN}✅ HEALTHY${NC}"
        else
            echo -e "Overall Status: ${RED}❌ UNHEALTHY${NC}"
        fi
        
        local healthy_count=0
        local total_count=${#results[@]}
        
        for result in "${results[@]}"; do
            IFS=',' read -r service container_status endpoint_status <<< "$result"
            if [[ "$container_status" == "healthy" || "$container_status" == "running" ]] && [[ "$endpoint_status" == "healthy" || "$endpoint_status" == "unknown" ]]; then
                ((healthy_count++))
            fi
        done
        
        echo "Services: $healthy_count/$total_count healthy"
        echo "Checked at: $(date)"
    fi
    
    # Return exit code based on overall status
    if [[ "$overall_status" == "healthy" ]]; then
        return 0
    else
        return 1
    fi
}

# Main execution
main() {
    cd "$PROJECT_DIR"
    
    # Check if environment files exist
    if [[ ! -f "$COMPOSE_FILE" ]]; then
        echo -e "${RED}❌ Compose file not found: $COMPOSE_FILE${NC}"
        exit 1
    fi
    
    if [[ ! -f "$ENV_FILE" ]]; then
        echo -e "${RED}❌ Environment file not found: $ENV_FILE${NC}"
        exit 1
    fi
    
    if [[ "$CONTINUOUS" == "true" ]]; then
        if [[ "$JSON_OUTPUT" != "true" ]]; then
            echo -e "${BLUE}🔄 Starting continuous health monitoring (every ${INTERVAL}s)${NC}"
            echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
            echo ""
        fi
        
        while true; do
            perform_health_checks
            
            if [[ "$JSON_OUTPUT" != "true" ]]; then
                echo ""
                echo -e "${YELLOW}Waiting ${INTERVAL} seconds...${NC}"
                echo ""
            fi
            
            sleep "$INTERVAL"
        done
    else
        perform_health_checks
    fi
}

# Handle Ctrl+C gracefully
trap 'echo -e "\n${YELLOW}Stopping health check...${NC}"; exit 0' INT

# Execute main function
main "$@"