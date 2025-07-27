#!/bin/bash

# Mini-UPS Local Deployment Test Script
# Test the unified Docker deployment locally before AWS deployment

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
TEST_DURATION=120  # seconds

# Function to log with timestamp
log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Function to cleanup on exit
cleanup() {
    echo ""
    log "Cleaning up test environment..."
    cd "$PROJECT_DIR"
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down -v 2>/dev/null || true
    docker system prune -f >/dev/null 2>&1 || true
    echo -e "${GREEN}✅ Cleanup completed${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Docker
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}❌ Docker is not running${NC}"
        exit 1
    fi
    
    # Check Docker Compose
    if ! docker-compose --version >/dev/null 2>&1; then
        echo -e "${RED}❌ Docker Compose is not available${NC}"
        exit 1
    fi
    
    # Check required files
    if [[ ! -f "$COMPOSE_FILE" ]]; then
        echo -e "${RED}❌ Compose file not found: $COMPOSE_FILE${NC}"
        exit 1
    fi
    
    if [[ ! -f "$ENV_FILE" ]]; then
        echo -e "${RED}❌ Environment file not found: $ENV_FILE${NC}"
        exit 1
    fi
    
    # Check for required source directories
    local required_dirs=("backend" "frontend" "knowledge/amazon" "knowledge/world_simulator_exec/docker_deploy")
    for dir in "${required_dirs[@]}"; do
        if [[ ! -d "$dir" ]]; then
            echo -e "${RED}❌ Required directory not found: $dir${NC}"
            exit 1
        fi
    done
    
    echo -e "${GREEN}✅ Prerequisites check passed${NC}"
}

# Function to build images
build_images() {
    log "Building Docker images..."
    
    cd "$PROJECT_DIR"
    
    # Build all services
    if docker-compose -f "$COMPOSE_FILE" build --no-cache; then
        echo -e "${GREEN}✅ All images built successfully${NC}"
    else
        echo -e "${RED}❌ Image build failed${NC}"
        return 1
    fi
    
    # List built images
    echo -e "${BLUE}Built images:${NC}"
    docker images | grep -E "(mini-ups|mini-amazon|mini-world)" || echo "No matching images found"
}

# Function to start services
start_services() {
    log "Starting all services..."
    
    cd "$PROJECT_DIR"
    
    # Start services
    if docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d; then
        echo -e "${GREEN}✅ Services started${NC}"
    else
        echo -e "${RED}❌ Failed to start services${NC}"
        return 1
    fi
    
    # Show running containers
    echo -e "${BLUE}Running containers:${NC}"
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
}

# Function to wait for services
wait_for_services() {
    log "Waiting for services to be ready..."
    
    local max_wait=180  # 3 minutes
    local wait_time=0
    local check_interval=10
    
    while [[ $wait_time -lt $max_wait ]]; do
        local all_ready=true
        
        # Check each service
        local services=(
            "UPS Backend:http://localhost:8081/actuator/health"
            "UPS Frontend:http://localhost:3000"
            "Amazon Service:http://localhost:8080"
        )
        
        for service in "${services[@]}"; do
            local name="${service%%:*}"
            local url="${service##*:}"
            
            if curl -f -s -m 5 "$url" >/dev/null 2>&1; then
                echo -e "${GREEN}✅ $name is ready${NC}"
            else
                echo -e "${YELLOW}⏳ Waiting for $name...${NC}"
                all_ready=false
            fi
        done
        
        if [[ "$all_ready" == "true" ]]; then
            echo -e "${GREEN}✅ All services are ready${NC}"
            return 0
        fi
        
        sleep $check_interval
        wait_time=$((wait_time + check_interval))
        
        # Show progress
        echo -e "${BLUE}Progress: ${wait_time}s / ${max_wait}s${NC}"
    done
    
    echo -e "${RED}❌ Services did not become ready in time${NC}"
    return 1
}

# Function to run health checks
run_health_checks() {
    log "Running comprehensive health checks..."
    
    local failed_checks=0
    
    # HTTP endpoint checks
    local endpoints=(
        "UPS Backend Health:http://localhost:8081/actuator/health"
        "UPS Frontend:http://localhost:3000"
        "Amazon Service:http://localhost:8080"
        "RabbitMQ Management:http://localhost:15672"
    )
    
    echo -e "${BLUE}HTTP Endpoint Checks:${NC}"
    for endpoint in "${endpoints[@]}"; do
        local name="${endpoint%%:*}"
        local url="${endpoint##*:}"
        
        if curl -f -s -m 10 "$url" >/dev/null 2>&1; then
            echo -e "  ${GREEN}✅ $name${NC}"
        else
            echo -e "  ${RED}❌ $name${NC}"
            ((failed_checks++))
        fi
    done
    
    # Database connectivity checks
    echo -e "${BLUE}Database Connectivity Checks:${NC}"
    local databases=(
        "UPS Database:mini-ups-postgres"
        "Amazon Database:mini-amazon-db"
        "World Database:world-simulator-db"
    )
    
    for db in "${databases[@]}"; do
        local name="${db%%:*}"
        local container="${db##*:}"
        
        if docker exec "$container" pg_isready -U postgres >/dev/null 2>&1; then
            echo -e "  ${GREEN}✅ $name${NC}"
        else
            echo -e "  ${RED}❌ $name${NC}"
            ((failed_checks++))
        fi
    done
    
    # Redis check
    echo -e "${BLUE}Cache Service Checks:${NC}"
    if docker exec mini-ups-redis redis-cli ping >/dev/null 2>&1; then
        echo -e "  ${GREEN}✅ Redis Cache${NC}"
    else
        echo -e "  ${RED}❌ Redis Cache${NC}"
        ((failed_checks++))
    fi
    
    # Network connectivity checks
    echo -e "${BLUE}Network Connectivity Checks:${NC}"
    
    # Check if UPS can reach Amazon
    if docker exec mini-ups-backend curl -f -s -m 5 http://amazon-web:8080 >/dev/null 2>&1; then
        echo -e "  ${GREEN}✅ UPS → Amazon communication${NC}"
    else
        echo -e "  ${RED}❌ UPS → Amazon communication${NC}"
        ((failed_checks++))
    fi
    
    # Check TCP ports for World Simulator
    if netstat -ln 2>/dev/null | grep -q ":12345.*LISTEN" && netstat -ln 2>/dev/null | grep -q ":23456.*LISTEN"; then
        echo -e "  ${GREEN}✅ World Simulator TCP ports (12345, 23456)${NC}"
    else
        echo -e "  ${RED}❌ World Simulator TCP ports${NC}"
        ((failed_checks++))
    fi
    
    echo ""
    if [[ $failed_checks -eq 0 ]]; then
        echo -e "${GREEN}✅ All health checks passed ($failed_checks failed)${NC}"
        return 0
    else
        echo -e "${RED}❌ Health checks failed ($failed_checks failed)${NC}"
        return 1
    fi
}

# Function to run integration tests
run_integration_tests() {
    log "Running integration tests..."
    
    echo -e "${BLUE}Integration Test Suite:${NC}"
    
    # Test 1: UPS API authentication
    echo -e "${BLUE}Test 1: UPS API Authentication${NC}"
    local auth_response=$(curl -s -X POST "http://localhost:8081/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}' || echo "")
    
    if [[ "$auth_response" == *"token"* ]]; then
        echo -e "  ${GREEN}✅ UPS API authentication working${NC}"
    else
        echo -e "  ${RED}❌ UPS API authentication failed${NC}"
    fi
    
    # Test 2: Amazon service response
    echo -e "${BLUE}Test 2: Amazon Service Response${NC}"
    local amazon_response=$(curl -s "http://localhost:8080" || echo "")
    
    if [[ "$amazon_response" == *"amazon"* ]] || [[ "$amazon_response" == *"login"* ]] || [[ "$amazon_response" == *"html"* ]]; then
        echo -e "  ${GREEN}✅ Amazon service responding${NC}"
    else
        echo -e "  ${RED}❌ Amazon service not responding properly${NC}"
    fi
    
    # Test 3: Database queries
    echo -e "${BLUE}Test 3: Database Queries${NC}"
    local db_test_failed=false
    
    # Test UPS database
    if docker exec mini-ups-postgres psql -U postgres -d ups_db -c "SELECT 1;" >/dev/null 2>&1; then
        echo -e "  ${GREEN}✅ UPS database query${NC}"
    else
        echo -e "  ${RED}❌ UPS database query${NC}"
        db_test_failed=true
    fi
    
    # Test Amazon database
    if docker exec mini-amazon-db psql -U postgres -d mini_amazon -c "SELECT 1;" >/dev/null 2>&1; then
        echo -e "  ${GREEN}✅ Amazon database query${NC}"
    else
        echo -e "  ${RED}❌ Amazon database query${NC}"
        db_test_failed=true
    fi
    
    # Test World database
    if docker exec world-simulator-db psql -U postgres -d worldSim -c "SELECT 1;" >/dev/null 2>&1; then
        echo -e "  ${GREEN}✅ World database query${NC}"
    else
        echo -e "  ${RED}❌ World database query${NC}"
        db_test_failed=true
    fi
    
    # Test 4: Redis operations
    echo -e "${BLUE}Test 4: Redis Operations${NC}"
    if docker exec mini-ups-redis redis-cli set test-key "test-value" >/dev/null 2>&1 && \
       docker exec mini-ups-redis redis-cli get test-key >/dev/null 2>&1; then
        echo -e "  ${GREEN}✅ Redis operations${NC}"
        docker exec mini-ups-redis redis-cli del test-key >/dev/null 2>&1
    else
        echo -e "  ${RED}❌ Redis operations${NC}"
    fi
    
    echo ""
    if [[ "$db_test_failed" == "true" ]]; then
        echo -e "${RED}❌ Integration tests completed with failures${NC}"
        return 1
    else
        echo -e "${GREEN}✅ Integration tests passed${NC}"
        return 0
    fi
}

# Function to show resource usage
show_resource_usage() {
    log "Checking resource usage..."
    
    echo -e "${BLUE}Container Resource Usage:${NC}"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" 2>/dev/null || {
        echo -e "${YELLOW}⚠️  Could not get resource stats${NC}"
    }
    
    echo ""
    echo -e "${BLUE}System Resource Usage:${NC}"
    echo "Disk usage: $(df -h / | tail -1 | awk '{print $5 " used"}')"
    echo "Memory usage: $(free -h | grep Mem | awk '{print $3 "/" $2}')"
    
    # Check Docker system usage
    echo -e "${BLUE}Docker System Usage:${NC}"
    docker system df 2>/dev/null || echo "Could not get Docker system usage"
}

# Function to show access URLs
show_access_urls() {
    echo ""
    echo -e "${BLUE}🌐 Service Access URLs:${NC}"
    echo -e "   UPS Frontend:        http://localhost:3000"
    echo -e "   Amazon Service:      http://localhost:8080"
    echo -e "   UPS API:            http://localhost:8081/api"
    echo -e "   UPS Health Check:   http://localhost:8081/actuator/health"
    echo -e "   RabbitMQ Management: http://localhost:15672 (guest/guest)"
    echo ""
    echo -e "${BLUE}📊 Database Access:${NC}"
    echo -e "   UPS Database:        localhost:5431 (ups_db/postgres/abc123)"
    echo -e "   Amazon Database:     localhost:15432 (mini_amazon/postgres/abc123)"
    echo -e "   World Database:      localhost:5433 (worldSim/postgres/abc123)"
    echo -e "   Redis Cache:         localhost:6380"
    echo ""
}

# Function to run stress test
run_stress_test() {
    log "Running basic stress test for $TEST_DURATION seconds..."
    
    local test_start=$(date +%s)
    local test_end=$((test_start + TEST_DURATION))
    local request_count=0
    local failed_requests=0
    
    echo -e "${BLUE}Stress testing endpoints...${NC}"
    
    while [[ $(date +%s) -lt $test_end ]]; do
        # Test UPS health endpoint
        if curl -f -s -m 2 "http://localhost:8081/actuator/health" >/dev/null 2>&1; then
            ((request_count++))
        else
            ((failed_requests++))
        fi
        
        # Test UPS frontend
        if curl -f -s -m 2 "http://localhost:3000" >/dev/null 2>&1; then
            ((request_count++))
        else
            ((failed_requests++))
        fi
        
        # Test Amazon service
        if curl -f -s -m 2 "http://localhost:8080" >/dev/null 2>&1; then
            ((request_count++))
        else
            ((failed_requests++))
        fi
        
        sleep 1
    done
    
    local success_rate
    if [[ $((request_count + failed_requests)) -gt 0 ]]; then
        success_rate=$(( (request_count * 100) / (request_count + failed_requests) ))
    else
        success_rate=0
    fi
    
    echo -e "${BLUE}Stress Test Results:${NC}"
    echo "  Duration: $TEST_DURATION seconds"
    echo "  Successful requests: $request_count"
    echo "  Failed requests: $failed_requests"
    echo "  Success rate: ${success_rate}%"
    
    if [[ $success_rate -gt 90 ]]; then
        echo -e "  ${GREEN}✅ Stress test passed (${success_rate}% success)${NC}"
        return 0
    else
        echo -e "  ${RED}❌ Stress test failed (${success_rate}% success)${NC}"
        return 1
    fi
}

# Main execution
main() {
    echo -e "${BLUE}🧪 Mini-UPS Local Deployment Test${NC}"
    echo -e "${BLUE}═══════════════════════════════════${NC}"
    echo "Test duration: $TEST_DURATION seconds"
    echo "Compose file: $COMPOSE_FILE"
    echo "Environment: $ENV_FILE"
    echo ""
    
    cd "$PROJECT_DIR"
    
    # Set trap for cleanup
    trap cleanup EXIT INT TERM
    
    # Run test phases
    local test_failed=false
    
    # Phase 1: Prerequisites
    echo -e "${YELLOW}Phase 1: Prerequisites Check${NC}"
    if ! check_prerequisites; then
        test_failed=true
    fi
    echo ""
    
    # Phase 2: Build
    echo -e "${YELLOW}Phase 2: Build Images${NC}"
    if ! build_images; then
        test_failed=true
    fi
    echo ""
    
    # Phase 3: Start services
    echo -e "${YELLOW}Phase 3: Start Services${NC}"
    if ! start_services; then
        test_failed=true
    fi
    echo ""
    
    # Phase 4: Wait for readiness
    echo -e "${YELLOW}Phase 4: Wait for Services${NC}"
    if ! wait_for_services; then
        test_failed=true
    fi
    echo ""
    
    # Phase 5: Health checks
    echo -e "${YELLOW}Phase 5: Health Checks${NC}"
    if ! run_health_checks; then
        test_failed=true
    fi
    echo ""
    
    # Phase 6: Integration tests
    echo -e "${YELLOW}Phase 6: Integration Tests${NC}"
    if ! run_integration_tests; then
        test_failed=true
    fi
    echo ""
    
    # Phase 7: Resource usage
    echo -e "${YELLOW}Phase 7: Resource Usage${NC}"
    show_resource_usage
    echo ""
    
    # Phase 8: Stress test
    echo -e "${YELLOW}Phase 8: Stress Test${NC}"
    if ! run_stress_test; then
        test_failed=true
    fi
    echo ""
    
    # Show access information
    show_access_urls
    
    # Final results
    echo -e "${BLUE}📋 Test Results Summary${NC}"
    echo "─────────────────────────────────────────"
    
    if [[ "$test_failed" == "true" ]]; then
        echo -e "${RED}❌ Local deployment test FAILED${NC}"
        echo -e "${YELLOW}Check the logs above for details${NC}"
        echo -e "${YELLOW}Services will be cleaned up automatically${NC}"
        exit 1
    else
        echo -e "${GREEN}✅ Local deployment test PASSED${NC}"
        echo -e "${GREEN}All services are working correctly${NC}"
        echo -e "${BLUE}Services will be cleaned up in 30 seconds...${NC}"
        
        # Wait a bit before cleanup
        sleep 30
    fi
}

# Execute main function
main "$@"