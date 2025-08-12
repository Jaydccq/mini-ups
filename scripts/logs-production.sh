#!/bin/bash

# Mini-UPS Production Logs Viewer
# View logs for all services or specific services

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
ENV_FILE=".env.production.deploy"

# Fallback to default env file if deploy version doesn't exist
if [[ ! -f "$PROJECT_DIR/$ENV_FILE" && -f "$PROJECT_DIR/.env.production" ]]; then
    ENV_FILE=".env.production"
fi

# Default values
FOLLOW=false
TAIL_LINES=100
SERVICE=""

# Usage function
usage() {
    echo -e "${BLUE}Mini-UPS Production Logs Viewer${NC}"
    echo ""
    echo "Usage: $0 [OPTIONS] [SERVICE]"
    echo ""
    echo "Options:"
    echo "  -f, --follow      Follow log output (like tail -f)"
    echo "  -n, --lines N     Number of lines to show (default: 100)"
    echo "  -h, --help        Show this help message"
    echo ""
    echo "Services:"
    echo "  ups-backend       UPS Spring Boot backend"
    echo "  ups-frontend      UPS React frontend"
    echo "  ups-database      UPS PostgreSQL database"
    echo "  ups-redis         UPS Redis cache"
    echo "  ups-rabbitmq      UPS RabbitMQ message queue"
    echo "  amazon-web        Amazon Flask web service"
    echo "  amazon-db         Amazon PostgreSQL database"
    echo "  world-server      World Simulator server"
    echo "  world-db          World Simulator PostgreSQL database"
    echo ""
    echo "Examples:"
    echo "  $0                      # Show last 100 lines from all services"
    echo "  $0 -f                   # Follow logs from all services"
    echo "  $0 ups-backend          # Show UPS backend logs"
    echo "  $0 -f ups-backend       # Follow UPS backend logs"
    echo "  $0 -n 50 amazon-web     # Show last 50 lines from Amazon service"
    echo ""
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--follow)
            FOLLOW=true
            shift
            ;;
        -n|--lines)
            TAIL_LINES="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        -*)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
        *)
            SERVICE="$1"
            shift
            ;;
    esac
done

# Function to check if services are running
check_services() {
    if ! docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps -q >/dev/null 2>&1; then
        echo -e "${RED}❌ No services are running${NC}"
        echo -e "${YELLOW}Run ./start-production.sh to start services${NC}"
        exit 1
    fi
}

# Function to list available services
list_services() {
    echo -e "${BLUE}📋 Running Services:${NC}"
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
    echo ""
}

# Function to show service logs
show_logs() {
    local service_filter="$1"
    local follow_flag="$2"
    local tail_flag="$3"
    
    echo -e "${BLUE}📄 Showing logs for: ${service_filter:-"all services"}${NC}"
    echo -e "${BLUE}Follow mode: ${follow_flag}${NC}"
    echo -e "${BLUE}Lines: ${tail_flag}${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
    
    # Build docker-compose logs command
    local cmd="docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE logs"
    
    if [[ "$follow_flag" == "true" ]]; then
        cmd="$cmd -f"
    fi
    
    if [[ -n "$tail_flag" && "$follow_flag" != "true" ]]; then
        cmd="$cmd --tail=$tail_flag"
    fi
    
    if [[ -n "$service_filter" ]]; then
        cmd="$cmd $service_filter"
    fi
    
    # Execute the command
    eval $cmd
}

# Function to show system logs (if available)
show_system_logs() {
    echo -e "${YELLOW}📋 System Logs:${NC}"
    
    # Check for application logs
    local log_paths=(
        "/var/log/mini-ups"
        "$HOME/.local/log/mini-ups"
        "$PROJECT_DIR/logs"
    )
    
    for log_path in "${log_paths[@]}"; do
        if [[ -d "$log_path" ]]; then
            echo -e "${BLUE}Found logs in: $log_path${NC}"
            ls -la "$log_path" 2>/dev/null || true
            
            # Show recent application logs if available
            if [[ -f "$log_path/mini-ups.log" ]]; then
                echo -e "${BLUE}Recent application logs:${NC}"
                tail -n 20 "$log_path/mini-ups.log" 2>/dev/null || true
                echo ""
            fi
        fi
    done
}

# Function to analyze logs for errors
analyze_logs() {
    echo -e "${YELLOW}🔍 Analyzing logs for errors...${NC}"
    
    # Get recent logs and look for error patterns
    local temp_log=$(mktemp)
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=500 > "$temp_log" 2>/dev/null || {
        echo -e "${RED}❌ Failed to retrieve logs${NC}"
        rm -f "$temp_log"
        return 1
    }
    
    # Look for common error patterns
    local error_patterns=(
        "ERROR"
        "FATAL"
        "Exception"
        "failed"
        "error"
        "ERROR"
        "WARN"
    )
    
    local found_errors=false
    
    for pattern in "${error_patterns[@]}"; do
        local count=$(grep -i "$pattern" "$temp_log" 2>/dev/null | wc -l)
        if [[ $count -gt 0 ]]; then
            found_errors=true
            echo -e "${RED}Found $count instances of '$pattern'${NC}"
            
            # Show a few examples
            echo -e "${YELLOW}Examples:${NC}"
            grep -i "$pattern" "$temp_log" | head -3 | while IFS= read -r line; do
                echo "  $line"
            done
            echo ""
        fi
    done
    
    if [[ "$found_errors" == "false" ]]; then
        echo -e "${GREEN}✅ No obvious errors found in recent logs${NC}"
    fi
    
    rm -f "$temp_log"
}

# Function to show log statistics
show_log_stats() {
    echo -e "${BLUE}📊 Log Statistics:${NC}"
    
    # Get services list
    local services=$(docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" config --services 2>/dev/null || echo "")
    
    if [[ -z "$services" ]]; then
        echo -e "${RED}❌ Could not get services list${NC}"
        return 1
    fi
    
    echo -e "${BLUE}Service${NC}\t\t${BLUE}Log Lines${NC}\t${BLUE}Size${NC}"
    echo "─────────────────────────────────────────"
    
    for service in $services; do
        local container_id=$(docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps -q "$service" 2>/dev/null || echo "")
        
        if [[ -n "$container_id" ]]; then
            local log_lines=$(docker logs "$container_id" 2>&1 | wc -l)
            local log_size=$(docker logs "$container_id" 2>&1 | wc -c | numfmt --to=iec)
            printf "%-15s\t%8s\t%8s\n" "$service" "$log_lines" "$log_size"
        else
            printf "%-15s\t%8s\t%8s\n" "$service" "N/A" "N/A"
        fi
    done
    
    echo ""
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
    
    # Check if services are running
    check_services
    
    # Show service list
    list_services
    
    # If no service specified and not following, show some analysis
    if [[ -z "$SERVICE" && "$FOLLOW" != "true" ]]; then
        show_log_stats
        echo ""
        analyze_logs
        echo ""
        show_system_logs
        echo ""
        echo -e "${BLUE}Recent logs from all services:${NC}"
        echo -e "${BLUE}═════════════════════════════${NC}"
    fi
    
    # Show logs
    show_logs "$SERVICE" "$FOLLOW" "$TAIL_LINES"
}

# Handle Ctrl+C gracefully
trap 'echo -e "\n${YELLOW}Stopping log viewer...${NC}"; exit 0' INT

# Execute main function
main "$@"