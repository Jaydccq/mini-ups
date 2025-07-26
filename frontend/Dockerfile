# =======================================
# Multi-stage Production Docker Build
# Mini-UPS Frontend Application
# =======================================

# ----------------------------------------
# Stage 1: Dependencies and Build
# ----------------------------------------
FROM --platform=linux/amd64 node:20-alpine AS dependencies

# Install system dependencies
RUN apk add --no-cache \
    git \
    python3 \
    make \
    g++ \
    libc6-compat

# Set working directory
WORKDIR /app

# Copy package files for dependency caching
COPY package*.json ./
COPY .nvmrc* ./

# Install all dependencies (including devDependencies for build)
RUN npm ci --ignore-scripts && \
    npm cache clean --force

# ----------------------------------------
# Stage 2: Application Build
# ----------------------------------------
FROM dependencies AS build

# Copy source code
COPY . .

# Build arguments for environment-specific builds
ARG NODE_ENV=production
ARG BUILD_MODE=production
ARG VITE_API_BASE_URL
ARG VITE_WS_BASE_URL
ARG VITE_APP_VERSION
ARG VITE_ENABLE_ANALYTICS=false
ARG VITE_SENTRY_DSN
ARG VITE_LOG_LEVEL=error

# Set environment variables
ENV NODE_ENV=$NODE_ENV
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_BASE_URL=$VITE_WS_BASE_URL
ENV VITE_APP_VERSION=$VITE_APP_VERSION
ENV VITE_ENABLE_ANALYTICS=$VITE_ENABLE_ANALYTICS
ENV VITE_SENTRY_DSN=$VITE_SENTRY_DSN
ENV VITE_LOG_LEVEL=$VITE_LOG_LEVEL

# Type check and lint
RUN npm run type-check && \
    npm run lint

# Build the application with specified mode
RUN if [ "$BUILD_MODE" = "staging" ]; then \
        npm run build:staging; \
    elif [ "$BUILD_MODE" = "production" ]; then \
        npm run build:production; \
    else \
        npm run build; \
    fi

# Run build analysis
RUN npm run size-limit || true

# ----------------------------------------
# Stage 3: Production Runtime
# ----------------------------------------
FROM --platform=linux/amd64 nginx:1.25-alpine AS production

# Install runtime dependencies and security updates
RUN apk add --no-cache \
    curl \
    jq \
    tzdata \
    tini \
    gettext \
    su-exec && \
    apk upgrade && \
    rm -rf /var/cache/apk/*

# Create non-root user for security
RUN addgroup -g 1001 -S miniups && \
    adduser -S miniups -u 1001 -G miniups

# Create necessary directories with proper permissions
RUN mkdir -p /var/cache/nginx /var/run/nginx /var/log/nginx && \
    rm -rf /usr/share/nginx/html/* && \
    rm -f /etc/nginx/conf.d/default.conf && \
    ln -sf /dev/stdout /var/log/nginx/access.log && \
    ln -sf /dev/stderr /var/log/nginx/error.log

# Copy nginx configuration template and entrypoint script
COPY --chown=miniups:miniups nginx.conf.template /etc/nginx/nginx.conf.template
COPY --chown=miniups:miniups scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

# Copy built application
COPY --from=build --chown=miniups:miniups /app/dist /usr/share/nginx/html

# Copy health check script
COPY --chown=miniups:miniups scripts/health-check.sh /usr/local/bin/health-check.sh

# Set proper permissions for non-root nginx operation
RUN chmod +x /usr/local/bin/health-check.sh && \
    chmod +x /usr/local/bin/docker-entrypoint.sh

# Create version info file
RUN echo "{\
  \"version\": \"${VITE_APP_VERSION:-1.0.0}\",\
  \"build_time\": \"$(date -Iseconds)\",\
  \"build_mode\": \"${BUILD_MODE:-production}\",\
  \"node_version\": \"$(node --version)\",\
  \"nginx_version\": \"$(nginx -v 2>&1 | cut -d' ' -f3)\"\
}" > /usr/share/nginx/html/version.json

# Note: nginx master runs as root, workers run as miniups user (standard nginx security model)

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD /usr/local/bin/health-check.sh

# Labels for metadata
LABEL maintainer="Mini-UPS Team"
LABEL description="Mini-UPS Frontend Application"
LABEL version="${VITE_APP_VERSION:-1.0.0}"

# Use tini for proper signal handling
ENTRYPOINT ["/sbin/tini", "--", "/usr/local/bin/docker-entrypoint.sh"]

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

# ----------------------------------------
# Development Stage (optional)
# ----------------------------------------
FROM dependencies AS development

# Copy source code
COPY . .

# Expose development port
EXPOSE 3000

# Development command
CMD ["npm", "run", "dev"]