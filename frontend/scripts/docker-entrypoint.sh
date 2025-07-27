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

# Set proper ownership for nginx worker processes (miniups user)
echo "Setting up directory permissions for nginx..."
chown -R miniups:miniups /var/cache/nginx /usr/share/nginx/html
# Keep log and pid directories owned by root (master process needs this)
chmod 755 /var/log/nginx /var/run/nginx

echo "✅ Nginx configuration generated successfully"

# Run nginx directly as root (master), workers will run as miniups user
exec "$@"