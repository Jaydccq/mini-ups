#!/bin/bash

echo "⏹️  Stopping Mini-UPS Complete System..."
echo "========================================"

# Stop all containers
echo "🐳 Stopping all containers..."
docker compose down

# Optional: Remove volumes (uncomment if needed)
# echo "🗑️  Removing volumes..."
# docker compose down -v

echo ""
echo "✅ All services stopped successfully!"
echo ""
echo "💡 Service Information:"
echo "├── All containers have been stopped"
echo "├── Network 'projectnet' is preserved"
echo "└── Data volumes are preserved"
echo ""
echo "🔄 To restart all services:"
echo "└── ./start-all.sh"
echo ""
echo "🗑️  To completely clean up (including data):"
echo "└── docker compose down -v --remove-orphans"