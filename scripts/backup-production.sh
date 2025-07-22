#!/bin/bash

# Mini-UPS Production Backup Script
# Backup databases and important data

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

# Default values
BACKUP_DIR="/var/backups/mini-ups"
RETENTION_DAYS=7
COMPRESS=true
BACKUP_TYPE="all"

# Usage function
usage() {
    echo -e "${BLUE}Mini-UPS Production Backup Script${NC}"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -d, --dir PATH        Backup directory (default: /var/backups/mini-ups)"
    echo "  -r, --retention N     Retention in days (default: 7)"
    echo "  -n, --no-compress     Don't compress backups"
    echo "  -t, --type TYPE       Backup type: all, databases, configs (default: all)"
    echo "  -h, --help            Show this help message"
    echo ""
    echo "Backup Types:"
    echo "  all         Backup databases, configurations, and logs"
    echo "  databases   Only backup database data"
    echo "  configs     Only backup configuration files"
    echo ""
    echo "Examples:"
    echo "  $0                           # Full backup with defaults"
    echo "  $0 -d /home/ubuntu/backups   # Custom backup directory"
    echo "  $0 -t databases              # Only databases"
    echo "  $0 -r 14 --no-compress       # 14-day retention, no compression"
    echo ""
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dir)
            BACKUP_DIR="$2"
            shift 2
            ;;
        -r|--retention)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        -n|--no-compress)
            COMPRESS=false
            shift
            ;;
        -t|--type)
            BACKUP_TYPE="$2"
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
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Function to create backup directory
create_backup_dir() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_path="$BACKUP_DIR/$timestamp"
    
    # Create directory with proper permissions
    if [[ "$EUID" -eq 0 ]]; then
        # Running as root
        mkdir -p "$backup_path"
        chown -R 1000:1000 "$backup_path" 2>/dev/null || true
    else
        # Running as non-root user, use sudo if needed
        if [[ ! -d "$BACKUP_DIR" ]]; then
            sudo mkdir -p "$BACKUP_DIR" 2>/dev/null || mkdir -p "$BACKUP_DIR"
            sudo chown -R $(id -u):$(id -g) "$BACKUP_DIR" 2>/dev/null || true
        fi
        mkdir -p "$backup_path"
    fi
    
    echo "$backup_path"
}

# Function to check if containers are running
check_containers() {
    local required_containers=("mini-ups-postgres" "mini-amazon-db" "world-simulator-db")
    
    for container in "${required_containers[@]}"; do
        if ! docker inspect "$container" >/dev/null 2>&1; then
            echo -e "${RED}❌ Container $container not found${NC}"
            return 1
        fi
        
        local status=$(docker inspect --format='{{.State.Status}}' "$container")
        if [[ "$status" != "running" ]]; then
            echo -e "${RED}❌ Container $container is not running (status: $status)${NC}"
            return 1
        fi
    done
    
    return 0
}

# Function to backup UPS database
backup_ups_database() {
    local backup_path="$1"
    
    log "Backing up UPS database..."
    
    docker exec mini-ups-postgres pg_dump \
        -U postgres \
        -d ups_db \
        --verbose \
        --no-owner \
        --no-privileges \
        > "$backup_path/ups_database.sql"
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ UPS database backup completed${NC}"
    else
        echo -e "${RED}❌ UPS database backup failed${NC}"
        return 1
    fi
}

# Function to backup Amazon database
backup_amazon_database() {
    local backup_path="$1"
    
    log "Backing up Amazon database..."
    
    docker exec mini-amazon-db pg_dump \
        -U postgres \
        -d mini_amazon \
        --verbose \
        --no-owner \
        --no-privileges \
        > "$backup_path/amazon_database.sql"
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Amazon database backup completed${NC}"
    else
        echo -e "${RED}❌ Amazon database backup failed${NC}"
        return 1
    fi
}

# Function to backup World Simulator database
backup_world_database() {
    local backup_path="$1"
    
    log "Backing up World Simulator database..."
    
    docker exec world-simulator-db pg_dump \
        -U postgres \
        -d worldSim \
        --verbose \
        --no-owner \
        --no-privileges \
        > "$backup_path/world_database.sql"
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ World Simulator database backup completed${NC}"
    else
        echo -e "${RED}❌ World Simulator database backup failed${NC}"
        return 1
    fi
}

# Function to backup Redis data
backup_redis_data() {
    local backup_path="$1"
    
    log "Backing up Redis data..."
    
    # Create Redis backup using BGSAVE
    docker exec mini-ups-redis redis-cli BGSAVE >/dev/null
    
    # Wait for backup to complete
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        local last_save=$(docker exec mini-ups-redis redis-cli LASTSAVE)
        sleep 1
        local current_save=$(docker exec mini-ups-redis redis-cli LASTSAVE)
        
        if [[ "$current_save" != "$last_save" ]]; then
            break
        fi
        
        ((attempt++))
        if [[ $attempt -gt $max_attempts ]]; then
            echo -e "${YELLOW}⚠️  Redis backup may still be in progress${NC}"
            break
        fi
    done
    
    # Copy Redis dump file
    docker cp mini-ups-redis:/data/dump.rdb "$backup_path/redis_dump.rdb" 2>/dev/null || {
        echo -e "${YELLOW}⚠️  Could not copy Redis dump file${NC}"
        return 1
    }
    
    echo -e "${GREEN}✅ Redis data backup completed${NC}"
}

# Function to backup configurations
backup_configurations() {
    local backup_path="$1"
    
    log "Backing up configurations..."
    
    local config_dir="$backup_path/configs"
    mkdir -p "$config_dir"
    
    # Backup main configuration files
    local config_files=(
        "$COMPOSE_FILE"
        "$ENV_FILE"
        "docker-compose.yml"
        ".env"
    )
    
    for config_file in "${config_files[@]}"; do
        if [[ -f "$config_file" ]]; then
            cp "$config_file" "$config_dir/" 2>/dev/null && \
            log "Backed up $config_file"
        fi
    done
    
    # Backup scripts directory
    if [[ -d "scripts" ]]; then
        cp -r "scripts" "$config_dir/" 2>/dev/null && \
        log "Backed up scripts directory"
    fi
    
    # Backup knowledge directory (but not the large simulator executables)
    if [[ -d "knowledge" ]]; then
        mkdir -p "$config_dir/knowledge"
        
        # Amazon configurations
        if [[ -d "knowledge/amazon" ]]; then
            find "knowledge/amazon" -name "*.py" -o -name "*.yml" -o -name "*.sql" -o -name "*.txt" | \
            while read -r file; do
                local target_dir="$config_dir/knowledge/$(dirname "$file")"
                mkdir -p "$target_dir"
                cp "$file" "$target_dir/" 2>/dev/null
            done
        fi
        
        log "Backed up knowledge configurations"
    fi
    
    echo -e "${GREEN}✅ Configurations backup completed${NC}"
}

# Function to backup logs
backup_logs() {
    local backup_path="$1"
    
    log "Backing up logs..."
    
    local logs_dir="$backup_path/logs"
    mkdir -p "$logs_dir"
    
    # Backup application logs
    local log_paths=(
        "/var/log/mini-ups"
        "$HOME/.local/log/mini-ups"
        "$PROJECT_DIR/logs"
    )
    
    for log_path in "${log_paths[@]}"; do
        if [[ -d "$log_path" ]]; then
            cp -r "$log_path"/* "$logs_dir/" 2>/dev/null && \
            log "Backed up logs from $log_path"
        fi
    done
    
    # Backup container logs (last 1000 lines each)
    local containers=("mini-ups-backend" "mini-ups-frontend" "mini-amazon-web" "world-simulator-server")
    
    for container in "${containers[@]}"; do
        if docker inspect "$container" >/dev/null 2>&1; then
            docker logs --tail 1000 "$container" > "$logs_dir/${container}.log" 2>&1 && \
            log "Backed up logs for $container"
        fi
    done
    
    echo -e "${GREEN}✅ Logs backup completed${NC}"
}

# Function to compress backup
compress_backup() {
    local backup_path="$1"
    
    if [[ "$COMPRESS" != "true" ]]; then
        return 0
    fi
    
    log "Compressing backup..."
    
    local backup_name=$(basename "$backup_path")
    local backup_parent=$(dirname "$backup_path")
    
    cd "$backup_parent"
    
    if tar -czf "${backup_name}.tar.gz" "$backup_name"; then
        rm -rf "$backup_name"
        echo -e "${GREEN}✅ Backup compressed to ${backup_name}.tar.gz${NC}"
        echo "$backup_parent/${backup_name}.tar.gz"
    else
        echo -e "${RED}❌ Compression failed${NC}"
        echo "$backup_path"
        return 1
    fi
}

# Function to cleanup old backups
cleanup_old_backups() {
    log "Cleaning up old backups (keeping last $RETENTION_DAYS days)..."
    
    if [[ ! -d "$BACKUP_DIR" ]]; then
        return 0
    fi
    
    # Find and remove old backups
    local deleted_count=0
    
    if [[ "$COMPRESS" == "true" ]]; then
        # Remove compressed backups
        while IFS= read -r -d '' backup_file; do
            rm -f "$backup_file"
            ((deleted_count++))
            log "Removed old backup: $(basename "$backup_file")"
        done < <(find "$BACKUP_DIR" -name "*.tar.gz" -type f -mtime +$RETENTION_DAYS -print0 2>/dev/null)
    else
        # Remove uncompressed backup directories
        while IFS= read -r -d '' backup_dir; do
            rm -rf "$backup_dir"
            ((deleted_count++))
            log "Removed old backup: $(basename "$backup_dir")"
        done < <(find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -mtime +$RETENTION_DAYS -print0 2>/dev/null)
    fi
    
    if [[ $deleted_count -gt 0 ]]; then
        echo -e "${GREEN}✅ Cleaned up $deleted_count old backups${NC}"
    else
        echo -e "${BLUE}ℹ️  No old backups to clean up${NC}"
    fi
}

# Function to show backup summary
show_backup_summary() {
    local backup_path="$1"
    
    echo ""
    echo -e "${BLUE}📋 Backup Summary${NC}"
    echo "─────────────────────────────────────────"
    echo "Backup location: $backup_path"
    echo "Backup type: $BACKUP_TYPE"
    echo "Compression: $COMPRESS"
    echo "Retention: $RETENTION_DAYS days"
    echo "Completed at: $(date)"
    
    # Show backup size
    if [[ -e "$backup_path" ]]; then
        local size
        if [[ -f "$backup_path" ]]; then
            size=$(du -h "$backup_path" | cut -f1)
        else
            size=$(du -sh "$backup_path" 2>/dev/null | cut -f1)
        fi
        echo "Backup size: ${size:-Unknown}"
    fi
    
    # List backup contents
    if [[ -d "$backup_path" ]]; then
        echo ""
        echo -e "${BLUE}📁 Backup Contents:${NC}"
        ls -lah "$backup_path"
    elif [[ -f "$backup_path" ]]; then
        echo ""
        echo -e "${BLUE}📦 Compressed Backup:${NC}"
        echo "$(basename "$backup_path")"
    fi
}

# Main execution
main() {
    cd "$PROJECT_DIR"
    
    echo -e "${BLUE}🗄️  Mini-UPS Production Backup${NC}"
    echo -e "${BLUE}═══════════════════════════════${NC}"
    echo "Backup type: $BACKUP_TYPE"
    echo "Target directory: $BACKUP_DIR"
    echo "Retention: $RETENTION_DAYS days"
    echo "Compression: $COMPRESS"
    echo ""
    
    # Check if environment files exist
    if [[ ! -f "$COMPOSE_FILE" ]]; then
        echo -e "${RED}❌ Compose file not found: $COMPOSE_FILE${NC}"
        exit 1
    fi
    
    # Create backup directory
    local backup_path=$(create_backup_dir)
    log "Created backup directory: $backup_path"
    
    # Check containers (only for database backups)
    if [[ "$BACKUP_TYPE" == "all" || "$BACKUP_TYPE" == "databases" ]]; then
        if ! check_containers; then
            echo -e "${RED}❌ Required containers are not running${NC}"
            echo -e "${YELLOW}Start services with: ./start-production.sh${NC}"
            exit 1
        fi
    fi
    
    # Perform backups based on type
    local backup_failed=false
    
    case $BACKUP_TYPE in
        "all")
            backup_ups_database "$backup_path" || backup_failed=true
            backup_amazon_database "$backup_path" || backup_failed=true
            backup_world_database "$backup_path" || backup_failed=true
            backup_redis_data "$backup_path" || backup_failed=true
            backup_configurations "$backup_path" || backup_failed=true
            backup_logs "$backup_path" || backup_failed=true
            ;;
        "databases")
            backup_ups_database "$backup_path" || backup_failed=true
            backup_amazon_database "$backup_path" || backup_failed=true
            backup_world_database "$backup_path" || backup_failed=true
            backup_redis_data "$backup_path" || backup_failed=true
            ;;
        "configs")
            backup_configurations "$backup_path" || backup_failed=true
            ;;
        *)
            echo -e "${RED}❌ Invalid backup type: $BACKUP_TYPE${NC}"
            exit 1
            ;;
    esac
    
    # Create backup manifest
    cat > "$backup_path/backup_manifest.txt" <<EOF
# Mini-UPS Backup Manifest
Backup Date: $(date)
Backup Type: $BACKUP_TYPE
Server: $(hostname)
UPS Version: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

# Backup Contents:
$(ls -la "$backup_path")

# System Info:
Docker Version: $(docker --version)
Compose Version: $(docker-compose --version)
Disk Usage: $(df -h / | tail -1)
Memory Usage: $(free -h | grep Mem)
EOF
    
    # Compress backup if requested
    if [[ "$COMPRESS" == "true" ]]; then
        backup_path=$(compress_backup "$backup_path")
    fi
    
    # Cleanup old backups
    cleanup_old_backups
    
    # Show summary
    show_backup_summary "$backup_path"
    
    if [[ "$backup_failed" == "true" ]]; then
        echo -e "${YELLOW}⚠️  Backup completed with some failures${NC}"
        exit 1
    else
        echo -e "${GREEN}✅ Backup completed successfully!${NC}"
    fi
}

# Execute main function
main "$@"