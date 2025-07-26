#!/bin/sh

# Docker entrypoint script for Mini-UPS Frontend
# Generates Nginx configuration from template with environment variables

set -e

# Default values
BACKEND_HOST=${BACKEND_HOST:-backend}
BACKEND_PORT=${BACKEND_PORT:-8081}

echo "🔧 Configuring Nginx with environment variables..."
echo "Backend Host: $BACKEND_HOST"
echo "Backend Port: $BACKEND_PORT"

# Generate nginx.conf from template using environment variable substitution
envsubst '${BACKEND_HOST} ${BACKEND_PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

# Copy to conf.d as well for compatibility
cp /etc/nginx/nginx.conf /etc/nginx/conf.d/default.conf

echo "✅ Nginx configuration generated successfully"

# Execute the command passed to docker run
exec "$@"