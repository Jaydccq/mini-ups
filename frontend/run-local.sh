#!/bin/bash

echo "=== Starting Mini-UPS Frontend (Local Development) ==="

# Check if node is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed"
    echo "Please install Node.js (v18 or higher) first"
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed"
    echo "Please install npm first"
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

echo "✅ Dependencies are ready"

# Check if backend is running
if ! curl -s http://localhost:8081/api/health &> /dev/null; then
    echo "⚠️  Backend is not running on http://localhost:8081"
    echo "Please start the backend first with: ./backend/run-local.sh"
    echo "Or start frontend anyway? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "🚀 Starting React development server..."
echo "📍 Environment: local development"
echo "🔗 API Backend: http://localhost:8081/api"
echo "🌐 Frontend: http://localhost:3000"
echo ""
echo "Hot reload is enabled - changes will be reflected automatically"
echo "Press Ctrl+C to stop the development server"
echo ""

npm run dev