#!/bin/bash

# =======================================
# Production Deployment Script
# Mini-UPS Frontend Application
# =======================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-ghcr.io/mini-ups}"
IMAGE_NAME="${IMAGE_NAME:-frontend}"
ENVIRONMENT="${1:-staging}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate environment
validate_environment() {
    case "$ENVIRONMENT" in
        staging|production)
            log_info "Deploying to $ENVIRONMENT environment"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT. Use 'staging' or 'production'"
            exit 1
            ;;
    esac
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker is not running"
        exit 1
    fi
    
    # Check if AWS CLI is installed (for ECS deployments)
    if ! command -v aws &> /dev/null; then
        log_warning "AWS CLI is not installed. Some deployment features may not work"
    fi
    
    # Check if required environment variables are set
    if [[ "$ENVIRONMENT" == "production" ]]; then
        required_vars=("AWS_ACCESS_KEY_ID" "AWS_SECRET_ACCESS_KEY" "AWS_DEFAULT_REGION")
        for var in "${required_vars[@]}"; do
            if [[ -z "${!var:-}" ]]; then
                log_error "Required environment variable $var is not set"
                exit 1
            fi
        done
    fi
    
    log_success "Prerequisites check passed"
}

# Build Docker image
build_image() {
    log_info "Building Docker image..."
    
    local image_tag="${DOCKER_REGISTRY}/${IMAGE_NAME}:${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S)"
    local latest_tag="${DOCKER_REGISTRY}/${IMAGE_NAME}:${ENVIRONMENT}-latest"
    
    # Build arguments based on environment
    local build_args=""
    case "$ENVIRONMENT" in
        staging)
            build_args="--build-arg BUILD_MODE=staging"
            build_args="$build_args --build-arg VITE_API_BASE_URL=${STAGING_API_URL:-https://api-staging.mini-ups.dev}"
            build_args="$build_args --build-arg VITE_WS_BASE_URL=${STAGING_WS_URL:-wss://api-staging.mini-ups.dev}"
            build_args="$build_args --build-arg VITE_ENABLE_ANALYTICS=false"
            build_args="$build_args --build-arg VITE_LOG_LEVEL=info"
            ;;
        production)
            build_args="--build-arg BUILD_MODE=production"
            build_args="$build_args --build-arg VITE_API_BASE_URL=${PRODUCTION_API_URL:-https://api.mini-ups.com}"
            build_args="$build_args --build-arg VITE_WS_BASE_URL=${PRODUCTION_WS_URL:-wss://api.mini-ups.com}"
            build_args="$build_args --build-arg VITE_ENABLE_ANALYTICS=true"
            build_args="$build_args --build-arg VITE_LOG_LEVEL=error"
            build_args="$build_args --build-arg VITE_SENTRY_DSN=${SENTRY_DSN:-}"
            ;;
    esac
    
    # Add common build args
    build_args="$build_args --build-arg VITE_APP_VERSION=${GIT_SHA:-$(git rev-parse HEAD)}"
    
    # Build the image
    if docker build \
        --platform linux/amd64 \
        --target production \
        $build_args \
        -t "$image_tag" \
        -t "$latest_tag" \
        "$PROJECT_DIR"; then
        log_success "Docker image built successfully: $image_tag"
        echo "$image_tag" > "${PROJECT_DIR}/.last-build-tag"
    else
        log_error "Failed to build Docker image"
        exit 1
    fi
}

# Push image to registry
push_image() {
    if [[ "${SKIP_PUSH:-false}" == "true" ]]; then
        log_info "Skipping image push (SKIP_PUSH=true)"
        return
    fi
    
    log_info "Pushing Docker image to registry..."
    
    local image_tag=$(cat "${PROJECT_DIR}/.last-build-tag")
    local latest_tag="${DOCKER_REGISTRY}/${IMAGE_NAME}:${ENVIRONMENT}-latest"
    
    # Login to registry if credentials are available
    if [[ -n "${DOCKER_USERNAME:-}" && -n "${DOCKER_PASSWORD:-}" ]]; then
        echo "$DOCKER_PASSWORD" | docker login "$DOCKER_REGISTRY" -u "$DOCKER_USERNAME" --password-stdin
    fi
    
    # Push both tags
    if docker push "$image_tag" && docker push "$latest_tag"; then
        log_success "Docker image pushed successfully"
    else
        log_error "Failed to push Docker image"
        exit 1
    fi
}

# Deploy to ECS
deploy_to_ecs() {
    if [[ "${SKIP_DEPLOY:-false}" == "true" ]]; then
        log_info "Skipping ECS deployment (SKIP_DEPLOY=true)"
        return
    fi
    
    log_info "Deploying to AWS ECS..."
    
    local cluster_name="mini-ups-${ENVIRONMENT}"
    local service_name="frontend-${ENVIRONMENT}"
    local image_tag=$(cat "${PROJECT_DIR}/.last-build-tag")
    
    # Update ECS service
    if aws ecs update-service \
        --cluster "$cluster_name" \
        --service "$service_name" \
        --force-new-deployment \
        --task-definition "$service_name" > /dev/null; then
        
        log_info "Waiting for deployment to complete..."
        
        if aws ecs wait services-stable \
            --cluster "$cluster_name" \
            --services "$service_name"; then
            log_success "ECS deployment completed successfully"
        else
            log_error "ECS deployment failed or timed out"
            exit 1
        fi
    else
        log_error "Failed to update ECS service"
        exit 1
    fi
}

# Run health checks
run_health_checks() {
    log_info "Running health checks..."
    
    local health_url
    case "$ENVIRONMENT" in
        staging)
            health_url="${STAGING_HEALTH_URL:-https://staging.mini-ups.dev/health}"
            ;;
        production)
            health_url="${PRODUCTION_HEALTH_URL:-https://mini-ups.com/health}"
            ;;
    esac
    
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        log_info "Health check attempt $attempt/$max_attempts..."
        
        if curl -f -s --max-time 10 "$health_url" > /dev/null; then
            log_success "Health check passed"
            return 0
        fi
        
        if [[ $attempt -lt $max_attempts ]]; then
            log_info "Health check failed, retrying in 10 seconds..."
            sleep 10
        fi
        
        ((attempt++))
    done
    
    log_error "Health checks failed after $max_attempts attempts"
    exit 1
}

# Cleanup old images
cleanup_old_images() {
    log_info "Cleaning up old Docker images..."
    
    # Remove old local images (keep last 5)
    local old_images=$(docker images "${DOCKER_REGISTRY}/${IMAGE_NAME}" --format "table {{.Repository}}:{{.Tag}}\t{{.CreatedAt}}" | \
        grep -E "${ENVIRONMENT}-[0-9]" | \
        sort -k2 -r | \
        tail -n +6 | \
        awk '{print $1}')
    
    if [[ -n "$old_images" ]]; then
        echo "$old_images" | xargs -r docker rmi || true
        log_success "Cleaned up old Docker images"
    else
        log_info "No old images to clean up"
    fi
}

# Send notification
send_notification() {
    local status="$1"
    local message="$2"
    
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        local color
        case "$status" in
            success) color="good" ;;
            error) color="danger" ;;
            *) color="warning" ;;
        esac
        
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"attachments\":[{\"color\":\"$color\",\"text\":\"$message\"}]}" \
            "$SLACK_WEBHOOK_URL" &> /dev/null || true
    fi
}

# Main deployment function
main() {
    log_info "Starting deployment process for Mini-UPS Frontend..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Timestamp: $(date)"
    
    # Store start time
    local start_time=$(date +%s)
    
    # Trap to handle errors
    trap 'log_error "Deployment failed!"; send_notification "error" "❌ Frontend deployment to $ENVIRONMENT failed"; exit 1' ERR
    
    # Run deployment steps
    validate_environment
    check_prerequisites
    build_image
    push_image
    deploy_to_ecs
    run_health_checks
    cleanup_old_images
    
    # Calculate deployment time
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log_success "Deployment completed successfully in ${duration}s"
    send_notification "success" "✅ Frontend deployed to $ENVIRONMENT successfully in ${duration}s"
    
    # Clean up temporary files
    rm -f "${PROJECT_DIR}/.last-build-tag"
}

# Handle script arguments
case "${1:-help}" in
    staging|production)
        main
        ;;
    help|--help|-h)
        echo "Usage: $0 [staging|production]"
        echo ""
        echo "Environment variables:"
        echo "  DOCKER_REGISTRY    - Docker registry URL (default: ghcr.io/mini-ups)"
        echo "  IMAGE_NAME         - Docker image name (default: frontend)"
        echo "  SKIP_PUSH          - Skip pushing to registry (default: false)"
        echo "  SKIP_DEPLOY        - Skip ECS deployment (default: false)"
        echo "  SLACK_WEBHOOK_URL  - Slack webhook for notifications"
        echo ""
        echo "AWS credentials required for production deployment:"
        echo "  AWS_ACCESS_KEY_ID"
        echo "  AWS_SECRET_ACCESS_KEY"
        echo "  AWS_DEFAULT_REGION"
        exit 0
        ;;
    *)
        log_error "Invalid argument: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac