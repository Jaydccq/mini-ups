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

# These operations need to be run as root before switching users
if [ "$(id -u)" = "0" ]; then
    # Generate nginx.conf from template using environment variable substitution
    envsubst '${BACKEND_HOST} ${BACKEND_PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

    # Copy to conf.d as well for compatibility
    cp /etc/nginx/nginx.conf /etc/nginx/conf.d/default.conf

    # Ensure nginx config is readable by miniups user
    chown miniups:miniups /etc/nginx/nginx.conf /etc/nginx/conf.d/default.conf

    # Take ownership of nginx runtime directories
    # This is critical for running as a non-root user, especially with volumes
    echo "Updating ownership of Nginx runtime directories for user 'miniups'..."
    chown -R miniups:miniups /var/cache/nginx /var/run/nginx

    echo "✅ Nginx configuration generated successfully"

    # Switch to miniups user and execute the command
    exec su-exec miniups "$@"
else
    # Already running as non-root user, just execute the command
    echo "⚠️  Already running as non-root user, skipping permission setup"
    exec "$@"
fi