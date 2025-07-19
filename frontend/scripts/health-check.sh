#!/bin/sh

# =======================================
# Health Check Script for Mini-UPS Frontend
# =======================================

set -e

# Configuration
HEALTH_URL="http://localhost:8080/health"
APP_URL="http://localhost:8080/"
TIMEOUT=5
MAX_RETRIES=3

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - HEALTH CHECK: $1"
}

# Check if nginx is running
check_nginx_process() {
    if ! pgrep -x "nginx" > /dev/null; then
        log "${RED}FAIL: Nginx process not running${NC}"
        return 1
    fi
    log "${GREEN}OK: Nginx process is running${NC}"
    return 0
}

# Check if port 8080 is listening
check_port() {
    if ! netstat -tln 2>/dev/null | grep -q ":8080 "; then
        # Fallback check using ss if netstat is not available
        if ! ss -tln 2>/dev/null | grep -q ":8080 "; then
            log "${RED}FAIL: Port 8080 is not listening${NC}"
            return 1
        fi
    fi
    log "${GREEN}OK: Port 8080 is listening${NC}"
    return 0
}

# Check health endpoint
check_health_endpoint() {
    local retry=0
    while [ $retry -lt $MAX_RETRIES ]; do
        if curl -f -s --max-time $TIMEOUT "$HEALTH_URL" > /dev/null 2>&1; then
            log "${GREEN}OK: Health endpoint is responding${NC}"
            return 0
        fi
        retry=$((retry + 1))
        if [ $retry -lt $MAX_RETRIES ]; then
            log "${YELLOW}WARN: Health endpoint check failed, retrying... ($retry/$MAX_RETRIES)${NC}"
            sleep 1
        fi
    done
    log "${RED}FAIL: Health endpoint is not responding after $MAX_RETRIES attempts${NC}"
    return 1
}

# Check main application endpoint
check_app_endpoint() {
    local retry=0
    while [ $retry -lt $MAX_RETRIES ]; do
        local response=$(curl -s --max-time $TIMEOUT -w "%{http_code}" -o /dev/null "$APP_URL")
        if [ "$response" = "200" ]; then
            log "${GREEN}OK: Application endpoint is responding (HTTP $response)${NC}"
            return 0
        fi
        retry=$((retry + 1))
        if [ $retry -lt $MAX_RETRIES ]; then
            log "${YELLOW}WARN: Application endpoint check failed (HTTP $response), retrying... ($retry/$MAX_RETRIES)${NC}"
            sleep 1
        fi
    done
    log "${RED}FAIL: Application endpoint is not responding properly after $MAX_RETRIES attempts${NC}"
    return 1
}

# Check static assets
check_static_assets() {
    local static_url="http://localhost:8080/assets/"
    local response=$(curl -s --max-time $TIMEOUT -w "%{http_code}" -o /dev/null "$static_url" || echo "000")
    
    # 403 is expected for directory listing, 200 is also fine
    if [ "$response" = "200" ] || [ "$response" = "403" ]; then
        log "${GREEN}OK: Static assets are accessible${NC}"
        return 0
    else
        log "${YELLOW}WARN: Static assets endpoint returned HTTP $response${NC}"
        # This is not a critical failure
        return 0
    fi
}

# Check disk space
check_disk_space() {
    local usage=$(df /usr/share/nginx/html | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$usage" -gt 90 ]; then
        log "${RED}FAIL: Disk usage is critical: ${usage}%${NC}"
        return 1
    elif [ "$usage" -gt 80 ]; then
        log "${YELLOW}WARN: Disk usage is high: ${usage}%${NC}"
    else
        log "${GREEN}OK: Disk usage is normal: ${usage}%${NC}"
    fi
    return 0
}

# Check memory usage
check_memory() {
    if [ -f /proc/meminfo ]; then
        local mem_total=$(grep MemTotal /proc/meminfo | awk '{print $2}')
        local mem_available=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
        local mem_usage=$((100 - (mem_available * 100 / mem_total)))
        
        if [ "$mem_usage" -gt 90 ]; then
            log "${RED}FAIL: Memory usage is critical: ${mem_usage}%${NC}"
            return 1
        elif [ "$mem_usage" -gt 80 ]; then
            log "${YELLOW}WARN: Memory usage is high: ${mem_usage}%${NC}"
        else
            log "${GREEN}OK: Memory usage is normal: ${mem_usage}%${NC}"
        fi
    else
        log "${YELLOW}WARN: Cannot check memory usage${NC}"
    fi
    return 0
}

# Main health check function
main() {
    log "Starting health check..."
    
    local exit_code=0
    
    # Critical checks - if these fail, container is unhealthy
    check_nginx_process || exit_code=1
    check_port || exit_code=1
    check_app_endpoint || exit_code=1
    
    # Non-critical checks - warnings only
    check_health_endpoint || log "${YELLOW}WARN: Health endpoint check failed (non-critical)${NC}"
    check_static_assets || log "${YELLOW}WARN: Static assets check failed (non-critical)${NC}"
    check_disk_space || exit_code=1
    check_memory || log "${YELLOW}WARN: Memory check failed (non-critical)${NC}"
    
    if [ $exit_code -eq 0 ]; then
        log "${GREEN}SUCCESS: All critical health checks passed${NC}"
        exit 0
    else
        log "${RED}FAILURE: One or more critical health checks failed${NC}"
        exit 1
    fi
}

# Ensure script runs with proper error handling
trap 'log "${RED}ERROR: Health check script failed unexpectedly${NC}"; exit 1' ERR

# Run main function
main "$@"