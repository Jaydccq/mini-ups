#!/bin/bash

# RabbitMQ Integration Test Script
# 
# This script helps test the RabbitMQ integration in the Mini-UPS system.
# It starts the services and runs basic tests to verify message processing.

set -e

echo "🚀 Mini-UPS RabbitMQ Integration Test"
echo "======================================"

# Configuration
BACKEND_URL="http://localhost:8081"
TEST_API="$BACKEND_URL/api/test/rabbitmq"

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo "⏳ Waiting for $service_name to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "✅ $service_name is ready!"
            return 0
        fi
        echo "   Attempt $attempt/$max_attempts - waiting..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "❌ $service_name failed to start within timeout"
    return 1
}

# Function to run a test
run_test() {
    local endpoint=$1
    local description=$2
    local data=$3
    
    echo ""
    echo "🧪 Testing: $description"
    echo "   Endpoint: $endpoint"
    
    if [ -n "$data" ]; then
        response=$(curl -s -X POST "$endpoint" -H "Content-Type: application/json" -d "$data" || echo "ERROR")
    else
        response=$(curl -s -X POST "$endpoint" || echo "ERROR")
    fi
    
    if echo "$response" | grep -q '"status":"success"'; then
        echo "   ✅ Test passed"
        echo "   📄 Response: $response" | head -c 200
        echo "..."
    else
        echo "   ❌ Test failed"
        echo "   📄 Response: $response"
    fi
}

# Function to check test endpoint availability
check_test_endpoint() {
    echo ""
    echo "🔍 Checking RabbitMQ test endpoint availability..."
    
    response=$(curl -s "$TEST_API/status" 2>/dev/null || echo "ERROR")
    
    if echo "$response" | grep -q '"status":"operational"'; then
        echo "✅ RabbitMQ test endpoints are available"
        return 0
    else
        echo "❌ RabbitMQ test endpoints are not available"
        echo "💡 Make sure RABBITMQ_TEST_ENABLED=true in your environment"
        echo "📄 Response: $response"
        return 1
    fi
}

# Main execution
main() {
    echo "🔧 Setting up test environment..."
    
    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        echo "❌ Docker is not running. Please start Docker first."
        exit 1
    fi
    
    # Start services
    echo "🐳 Starting Mini-UPS services with RabbitMQ..."
    export RABBITMQ_TEST_ENABLED=true
    export NOTIFICATIONS_ENABLED=true
    
    # Use docker compose to start services
    if docker compose up -d --build; then
        echo "✅ Services started successfully"
    else
        echo "❌ Failed to start services"
        exit 1
    fi
    
    # Wait for backend to be ready
    wait_for_service "$BACKEND_URL/api/health" "Mini-UPS Backend"
    
    # Wait a bit more for RabbitMQ to be fully ready
    echo "⏳ Waiting for RabbitMQ to be fully initialized..."
    sleep 10
    
    # Check if test endpoints are available
    if ! check_test_endpoint; then
        echo ""
        echo "💡 To enable RabbitMQ testing:"
        echo "   1. Set RABBITMQ_TEST_ENABLED=true in your .env file"
        echo "   2. Restart the services: docker compose restart backend"
        echo "   3. Run this script again"
        exit 1
    fi
    
    # Run tests
    echo ""
    echo "🎯 Running RabbitMQ Integration Tests"
    echo "===================================="
    
    # Test 1: Audit Log
    run_test "$TEST_API/test-audit?operationType=test.script.audit" "Audit Log Event"
    
    # Test 2: Notification
    run_test "$TEST_API/test-notification?userId=1&email=test@example.com&subject=RabbitMQ%20Test&message=Testing%20from%20script" "Notification Event"
    
    # Test 3: Shipment Creation
    run_test "$TEST_API/test-shipment?amazonShipmentId=99999&warehouseId=1&destX=100&destY=200&userId=1" "Shipment Creation Event"
    
    # Test 4: All Events
    run_test "$TEST_API/test-all" "All Event Types"
    
    echo ""
    echo "🎉 RabbitMQ Integration Tests Complete!"
    echo "======================================"
    echo ""
    echo "📊 To verify the tests:"
    echo "   1. Check RabbitMQ Management UI: http://localhost:15672 (guest/guest)"
    echo "   2. Check application logs: docker compose logs -f backend"
    echo "   3. Check database for audit logs"
    echo ""
    echo "🛠️  Available commands:"
    echo "   • View logs: docker compose logs -f"
    echo "   • Stop services: docker compose down"
    echo "   • Access RabbitMQ UI: open http://localhost:15672"
    echo "   • Access API docs: open http://localhost:8081/swagger-ui.html"
}

# Handle script arguments
case "${1:-}" in
    "help"|"-h"|"--help")
        echo "RabbitMQ Integration Test Script"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  help     Show this help message"
        echo "  status   Check service status"
        echo "  logs     Show service logs"
        echo "  stop     Stop all services"
        echo "  ui       Open RabbitMQ management UI"
        echo ""
        echo "Default: Run full integration test"
        ;;
    "status")
        echo "🔍 Checking service status..."
        docker compose ps
        echo ""
        if curl -s "$BACKEND_URL/api/health" > /dev/null 2>&1; then
            echo "✅ Backend is responding"
        else
            echo "❌ Backend is not responding"
        fi
        ;;
    "logs")
        echo "📊 Service logs:"
        docker compose logs -f
        ;;
    "stop")
        echo "🛑 Stopping services..."
        docker compose down
        echo "✅ Services stopped"
        ;;
    "ui")
        echo "🖥️  Opening RabbitMQ Management UI..."
        open "http://localhost:15672" 2>/dev/null || echo "Please open http://localhost:15672 in your browser"
        ;;
    *)
        main
        ;;
esac