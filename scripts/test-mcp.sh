#!/bin/bash

# Mini-UPS MCP Server Test Script
# Tests the MCP server functionality

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[MCP-TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[MCP-TEST]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[MCP-TEST]${NC} $1"
}

print_error() {
    echo -e "${RED}[MCP-TEST]${NC} $1"
}

# Check if we're in the right directory
if [ ! -d "mcp-server" ]; then
    print_error "Error: mcp-server directory not found. Please run this script from the project root directory."
    exit 1
fi

print_status "Running Mini-UPS MCP Server tests..."

# Change to MCP server directory
cd mcp-server

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    print_error "Node.js is not installed. Please install Node.js 20+ and try again."
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    print_error "npm is not installed. Please install npm and try again."
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 20 ]; then
    print_error "Node.js version 20 or higher is required. Current version: $(node --version)"
    exit 1
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    print_status "Installing dependencies..."
    npm ci
fi

# Parse command line arguments
TEST_TYPE="all"
COVERAGE=""
WATCH=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --unit)
            TEST_TYPE="unit"
            shift
            ;;
        --integration)
            TEST_TYPE="integration"
            shift
            ;;
        --coverage)
            COVERAGE="--coverage"
            shift
            ;;
        --watch)
            WATCH="--watch"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --unit         Run only unit tests"
            echo "  --integration  Run only integration tests"
            echo "  --coverage     Generate coverage report"
            echo "  --watch        Run tests in watch mode"
            echo "  --help,-h      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Run all tests"
            echo "  $0 --unit            # Run unit tests only"
            echo "  $0 --coverage        # Run tests with coverage"
            echo "  $0 --watch           # Run tests in watch mode"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information."
            exit 1
            ;;
    esac
done

# Run linting first
print_status "Running ESLint..."
if npm run lint; then
    print_success "Linting passed!"
else
    print_error "Linting failed!"
    exit 1
fi

# Run type checking
print_status "Running TypeScript type checking..."
if npm run type-check; then
    print_success "Type checking passed!"
else
    print_error "Type checking failed!"
    exit 1
fi

# Run tests based on type
case $TEST_TYPE in
    "unit")
        print_status "Running unit tests..."
        npm run test $COVERAGE $WATCH -- --run --testPathPattern="unit|__tests__"
        ;;
    "integration")
        print_status "Running integration tests..."
        # Check if backend is running for integration tests
        if ! curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
            print_warning "Backend is not running. Integration tests may fail."
            print_status "To start backend: cd .. && ./scripts/start-mcp.sh --mcp-only"
        fi
        npm run test $COVERAGE $WATCH -- --run --testPathPattern="integration"
        ;;
    "all")
        print_status "Running all tests..."
        npm run test $COVERAGE $WATCH -- --run
        ;;
esac

# Build the project to ensure it compiles
print_status "Building project..."
if npm run build; then
    print_success "Build successful!"
else
    print_error "Build failed!"
    exit 1
fi

print_success "All tests completed successfully!"

# Show coverage report location if coverage was generated
if [ -n "$COVERAGE" ]; then
    print_status "Coverage report generated at: mcp-server/coverage/index.html"
fi

print_status "MCP server is ready for deployment!"

cd ..