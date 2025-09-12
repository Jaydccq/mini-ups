# Mini-UPS NLQ MCP Server Dockerfile

# 1) Builder: install all deps (incl. dev) and compile TS
FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json* pnpm-lock.yaml* yarn.lock* ./
RUN if [ -f package-lock.json ]; then \
      npm ci; \
    else \
      npm install; \
    fi

COPY tsconfig.json ./
COPY tsconfig.build.json ./
COPY src ./src
RUN npm run build

# 2) Runtime: install only production deps and copy compiled output
FROM node:20-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production \
    LOG_LEVEL=info \
    LOG_FORMAT=json

COPY package.json package-lock.json* pnpm-lock.yaml* yarn.lock* ./
RUN if [ -f package-lock.json ]; then \
      npm ci --omit=dev; \
    else \
      npm install --omit=dev; \
    fi

COPY --from=builder /app/dist ./dist

CMD ["node", "dist/index.js"]
