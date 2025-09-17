#!/bin/bash

# Mini-UPS MCP Server Start Script
# Starts the Natural Language Query MCP server

set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Mini-UPS MCP Server Start Script ===${NC}"

# Check Node.js version
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed. Please install Node.js 20 or later.${NC}"
    exit 1
fi

NODE_VERSION=$(node -v | sed 's/v//')
REQUIRED_VERSION="20.0.0"

if ! node -p "
    const semver = (a, b) => {
        const pa = a.split('.').map(n => parseInt(n));
        const pb = b.split('.').map(n => parseInt(n));
        for (let i = 0; i < 3; i++) {
            if (pa[i] > pb[i]) return 1;
            if (pa[i] < pb[i]) return -1;
        }
        return 0;
    };
    semver('$NODE_VERSION', '$REQUIRED_VERSION') >= 0
" &> /dev/null; then
    echo -e "${RED}❌ Requires Node.js ${REQUIRED_VERSION} or later, current: ${NODE_VERSION}${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Node.js version check passed: ${NODE_VERSION}${NC}"

# Check environment file
if [[ ! -f ".env" ]]; then
    echo -e "${YELLOW}⚠️  No .env found; copying from .env.example...${NC}"
    cp .env.example .env
    echo -e "${YELLOW}📝 Edit .env to set your OPENROUTER_API_KEY${NC}"
    echo -e "${YELLOW}💡 Get an API key: https://openrouter.ai/keys${NC}"
fi

# Check OpenRouter API Key
source .env
if [[ "$OPENROUTER_API_KEY" == "your-openrouter-api-key-here" || -z "$OPENROUTER_API_KEY" ]]; then
    echo -e "${RED}❌ Please set a valid OPENROUTER_API_KEY in .env${NC}"
    echo -e "${YELLOW}💡 Get an API key: https://openrouter.ai/keys${NC}"
    exit 1
fi

# Check dependencies
if [[ ! -d "node_modules" ]]; then
    echo -e "${BLUE}📦 Installing NPM dependencies...${NC}"
    npm install
fi

# Build TypeScript
echo -e "${BLUE}🔨 Compiling TypeScript...${NC}"
if ! npm run build; then
    echo -e "${RED}❌ TypeScript build failed${NC}"
    exit 1
fi

# Check backend service availability
echo -e "${BLUE}🔍 Checking backend service connectivity...${NC}"
if ! curl -s -f "$BACKEND_BASE_URL/api/health" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Backend service ($BACKEND_BASE_URL) is unavailable${NC}"
    echo -e "${YELLOW}💡 Ensure the Mini-UPS backend is running${NC}"
    echo -e "${YELLOW}💡 Start it: cd ../backend && ./run-local.sh${NC}"
fi

# Start server
echo -e "${GREEN}🚀 Starting MCP Server...${NC}"
echo -e "${BLUE}📡 Server communicates with clients over stdio protocol${NC}"
echo -e "${BLUE}🔧 Config file: .env${NC}"
echo -e "${BLUE}📊 Log level: ${LOG_LEVEL:-info}${NC}"
echo ""

# Start in development or production mode
if [[ "$NODE_ENV" == "development" ]]; then
    echo -e "${YELLOW}🔥 Starting in development mode (hot reload)${NC}"
    exec npm run dev
else
    echo -e "${GREEN}🌟 Starting in production mode${NC}"
    exec npm run start
fi
