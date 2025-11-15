# Mini-UPS: Enterprise Distributed Logistics System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![AWS](https://img.shields.io/badge/AWS-Cloud%20Native-orange.svg)](https://aws.amazon.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

> Production-ready enterprise distributed logistics management system built with microservices architecture, featuring Protocol Buffer high-performance communication, real-time tracking, intelligent scheduling, RAG-powered AI assistance, and complete CI/CD pipeline with AWS cloud-native deployment capabilities.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [Core Modules](#core-modules)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Development](#development)
- [Performance](#performance)
- [Contributing](#contributing)
- [License](#license)

## Overview

Mini-UPS is a comprehensive logistics management system designed to handle package tracking, delivery scheduling, and real-time fleet management at enterprise scale. Built on modern microservices architecture, it demonstrates production-ready patterns including event-driven messaging, distributed caching, AI-powered assistance, and cloud-native deployment.

### What Makes Mini-UPS Different

- **High-Performance ID Generation**: Leaf-Segment algorithm achieving 50,000+ QPS with 1000x reduction in database queries
- **RAG-Powered AI Assistant**: Context-aware chatbot with hybrid semantic + keyword search, role-based rate limiting, and feedback loops
- **Write-Behind Caching**: 70% reduction in database write contention through intelligent batching
- **Transactional Outbox Pattern**: Guaranteed event delivery with at-least-once semantics
- **Protocol Buffer Communication**: Binary streaming with World Simulator for sub-millisecond latency
- **Multi-Environment Support**: Seamless deployment from local Docker to AWS ECS Fargate

## Key Features

### Core Capabilities

- **Intelligent Package Tracking**: High-performance tracking number generation (50,000+ QPS) with real-time status updates via WebSocket
- **Smart Truck Dispatching**: Distance-based optimization algorithm for fleet allocation with load balancing
- **Real-Time Updates**: STOMP over WebSocket with SockJS fallback, automatic reconnection, and pub/sub messaging
- **Multi-System Integration**: Seamless synchronization with Amazon e-commerce platform and World Simulator GPS tracking
- **AI-Powered Assistant**: RAG system with pgvector embeddings, OpenRouter LLM, and role-aware responses
- **Event-Driven Architecture**: Transactional Outbox pattern with RabbitMQ/Kafka support for reliable messaging
- **Enterprise Security**: JWT stateless authentication, BCrypt hashing, RBAC with 4 user roles, and API rate limiting

### Advanced Features

- **Distributed Caching**: Redis for distributed cache with Caffeine L1 cache for optimal performance
- **Comprehensive Monitoring**: Micrometer metrics, Prometheus endpoints, CloudWatch integration, and custom business metrics
- **High Availability**: Stateless design, horizontal scaling, read replicas, and automatic failover
- **Developer Experience**: Swagger/Knife4j API docs, comprehensive testing, local development scripts, and hot reload

## System Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Mini-UPS Enterprise System                       │
├─────────────────────────────────────────────────────────────────────┤
│  📱 Presentation Layer                                              │
│     React 18 + TypeScript + Radix UI + Tailwind CSS                 │
│     • Responsive UI with accessibility support                      │
│     • Real-time updates via WebSocket/STOMP                          │
│     • AI assistant with RAG-powered chatbot                          │
├─────────────────────────────────────────────────────────────────────┤
│  🌐 Gateway Layer                                                   │
│     Nginx + AWS ALB + SSL Termination                               │
│     • Load balancing with health checks                             │
│     • CORS & rate limiting                                          │
│     • API versioning & request routing                              │
├─────────────────────────────────────────────────────────────────────┤
│  🚀 Application Layer                                               │
│     ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐     │
│     │  🎮 Controllers │ │  💼 Services    │ │  🔒 Security    │     │
│     │  REST APIs      │ │  Business Logic │ │  JWT + OAuth2   │     │
│     │  WebSocket      │ │  Scheduling     │ │  RBAC           │     │
│     │  Webhooks       │ │  Integration    │ │  Rate Limiting  │     │
│     └─────────────────┘ └─────────────────┘ └─────────────────┘     │
├─────────────────────────────────────────────────────────────────────┤
│  📊 Data Access Layer                                               │
│     MyBatis 3.0 + Repository Pattern                                │
│     • Optimized SQL with parameter binding                          │
│     • Connection pooling (HikariCP)                                  │
│     • Transaction management                                         │
├─────────────────────────────────────────────────────────────────────┤
│  🗄️ Data Layer                                                     │
│     PostgreSQL 15 + pgvector | Redis 7 | RabbitMQ 3.13              │
│     • Primary database with vector support                           │
│     • Distributed cache with L1/L2 layers                            │
│     • Message broker with STOMP relay                                │
├─────────────────────────────────────────────────────────────────────┤
│  🔗 Integration Layer                                               │
│     Protocol Buffer + TCP + REST + WebSocket + Kafka                │
│     • Binary streaming for high performance                          │
│     • Event-driven messaging                                         │
│     • Transactional Outbox pattern                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### Distributed System Communication

```
┌─────────────────────────────────────────────────────────────────────┐
│                      External System Integration                    │
└─────────────────────────────────────────────────────────────────────┘
              │                    │                    │
    ┌─────────▼─────────┐ ┌────────▼────────┐ ┌────────▼────────┐
    │  World Simulator  │ │   UPS Core      │ │  Amazon         │
    │  (TCP:12345)      │ │ (Spring Boot)   │ │  (Flask API)    │
    │                   │ │                 │ │                 │
    │ 🌍 Capabilities:  │ │ 🚚 Features:    │ │ 🛒 Functions:   │
    │ • GPS Tracking    │ │ • Authentication│ │ • Order Mgmt    │
    │ • Route Planning  │ │ • Shipment Mgmt │ │ • Status Sync   │
    │ • Truck Sync      │◄┤ • Truck Dispatch│◄┤ • Address Mgmt  │
    │ • Warehouse Mgmt  │ │ • Real-time WS  │ │ • Webhooks      │
    │ • Environment Sim │ │ • RAG Assistant │ │ • Order Callback│
    │                   │ │ • Event Stream  │ │                 │
    │ Protocol Buffer   │ │ REST + WS + gRPC│ │ REST + Webhook  │
    │ Binary Stream     │ │ JSON/Protobuf   │ │ JSON/HTTP       │
    └───────────────────┘ └─────────────────┘ └─────────────────┘
                              │         │
                    ┌─────────┴─────┐   │
                    │               │   │
          ┌─────────▼──────┐ ┌──────▼───▼──────┐
          │  PostgreSQL 15 │ │    Redis 7      │
          │  + pgvector    │ │  Cache + Session│
          │                │ │                 │
          │ • Shipments    │ │ • User Sessions │
          │ • Users        │ │ • Cache Layer   │
          │ • Trucks       │ │ • Rate Limits   │
          │ • RAG Vectors  │ │ • Distributed   │
          │ • Audit Logs   │ │   Locks         │
          └────────────────┘ └─────────────────┘
```

### Event-Driven Messaging Architecture

```
┌──────────────────────────────────────────────────────────────┐
│              Transactional Outbox Pattern                    │
└──────────────────────────────────────────────────────────────┘

  1. Write Event to DB      2. Poll & Publish       3. Consume
  ┌─────────────┐          ┌──────────────┐        ┌──────────────┐
  │   Service   │──────────▶│ Outbox Table │        │  RabbitMQ    │
  │  Business   │          │              │        │   Exchange   │
  │   Logic     │          │ - eventId    │◄───────│              │
  └─────────────┘          │ - payload    │  Poll  │  ┌────────┐  │
                           │ - published  │────────▶│  │ Queue  │  │
                           └──────────────┘        │  └────┬───┘  │
                                                   │       │      │
                                                   └───────┼──────┘
                                                           │
                              ┌────────────────────────────┼───────────┐
                              │                            │           │
                    ┌─────────▼────────┐      ┌───────────▼──────┐   ┌▼──────────┐
                    │ Notification     │      │  Analytics       │   │ WebSocket │
                    │ Consumer         │      │  Consumer        │   │ Fan-out   │
                    └──────────────────┘      └──────────────────┘   └───────────┘
```

## Technology Stack

### Backend Technologies

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Framework** | Spring Boot | 3.3.2 | Microservices foundation |
| **Language** | Java | 17 | Application development |
| **ORM** | MyBatis | 3.0.3 | Data access layer |
| **Database** | PostgreSQL | 15 | Primary data store |
| **Vector DB** | pgvector | Latest | RAG embeddings |
| **Cache** | Redis | 7 | Distributed caching |
| **Message Broker** | RabbitMQ | 3.13 | Event messaging |
| **Streaming** | Apache Kafka | 3.x | Event streaming (optional) |
| **Security** | Spring Security | 6.4 | Authentication & authorization |
| **JWT** | jjwt | 0.12.3 | Token generation |
| **WebSocket** | Spring WebSocket + STOMP | 6.x | Real-time updates |
| **HTTP Client** | Apache HttpClient 5 | 5.3.1 | External API calls |
| **Serialization** | Protocol Buffers | 3.25.3 | Binary communication |
| **Network** | Netty | 4.1.110 | TCP client |
| **Local Cache** | Caffeine | Latest | L1 cache layer |
| **Distributed Lock** | Redisson | 3.26.1 | Distributed coordination |
| **Rate Limiting** | Alibaba Sentinel | 1.8.6 | API rate control |
| **ID Generation** | ShardingSphere | 5.3.2 | Distributed IDs |
| **Monitoring** | Micrometer + Prometheus | Latest | Metrics & observability |
| **API Docs** | SpringDoc OpenAPI + Knife4j | 2.6.0 / 4.4.0 | Interactive documentation |
| **Testing** | JUnit 5 + Mockito | Latest | Unit & integration tests |

### Frontend Technologies

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Framework** | React | 18.2.0 | UI library |
| **Language** | TypeScript | 5.2 | Type-safe development |
| **Build Tool** | Vite | 7.0.5 | Fast HMR & bundling |
| **Router** | React Router | 6.21.1 | Client-side routing |
| **State Management** | Zustand | 4.4.7 | Lightweight state |
| **Server State** | TanStack Query | 5.84.2 | Data fetching & caching |
| **UI Library** | Radix UI | 1.0 | Accessible components |
| **Styling** | Tailwind CSS | 3.4.1 | Utility-first CSS |
| **Form Validation** | React Hook Form + Zod | Latest | Type-safe forms |
| **HTTP Client** | Axios | 1.11.1 | API requests |
| **Real-Time** | Socket.IO + STOMP | 4.7.4 / 7.1.1 | WebSocket integration |
| **Charts** | Tremor React | 3.18.7 | Data visualization |
| **Icons** | Lucide React | 0.309.0 | Icon library |
| **Notifications** | Sonner | 2.0.6 | Toast notifications |
| **Testing** | Vitest + Testing Library | 3.2.4 | Component testing |

### Infrastructure & DevOps

| Technology | Purpose |
|-----------|---------|
| **Docker** | Containerization |
| **Kubernetes** | Container orchestration |
| **AWS ECS Fargate** | Serverless containers |
| **AWS RDS** | Managed PostgreSQL |
| **AWS ElastiCache** | Managed Redis |
| **AWS CloudWatch** | Logging & monitoring |
| **GitHub Actions** | CI/CD pipeline |
| **CloudFormation** | Infrastructure as code |
| **Nginx** | Reverse proxy & load balancer |
| **AWS ALB** | Application load balancing |
| **AWS Secrets Manager** | Credential management |

## Quick Start

### Prerequisites

```bash
# Required
Java 17+                    # OpenJDK recommended
Node.js 18+                 # For frontend development
Docker & Docker Compose     # For containerization
PostgreSQL 15+              # Database (or use Docker)
Redis 7+                    # Cache (or use Docker)

# Optional
Maven 3.9+                  # Build tool (included via wrapper)
kubectl                     # For Kubernetes deployment
AWS CLI                     # For cloud deployment
```

### Docker Deployment (Recommended)

```bash
# 1. Clone the repository
git clone <repository-url>
cd mini-ups

# 2. Configure environment (REQUIRED)
# Create .env file with required variables
cat > .env << EOF
JWT_SECRET=your-secret-key-minimum-32-characters-long
OPENROUTER_API_KEY=sk-or-your-openrouter-api-key
POSTGRES_PASSWORD=abc123
REDIS_PASSWORD=test123
EOF

# 3. Start all services
docker compose up --build

# That's it! System will be ready in 2-3 minutes
```

**Service Access:**

| Service | URL | Credentials |
|---------|-----|-------------|
| 🖥️ **Frontend UI** | http://localhost:3000 | Register new account |
| 🔌 **Backend API** | http://localhost:8081 | - |
| 📚 **API Docs (Swagger)** | http://localhost:8081/swagger-ui.html | - |
| 📖 **API Docs (Knife4j)** | http://localhost:8081/doc.html | - |
| 🛒 **Amazon System** | http://localhost:8080 | - |
| 🗄️ **PostgreSQL** | localhost:5431 | `postgres` / `abc123` |
| ⚡ **Redis** | localhost:6380 | Password: `test123` |
| 🐰 **RabbitMQ Management** | http://localhost:15672 | `guest` / `guest` |

### Local Development

```bash
# Check prerequisites
./start-local.sh --check

# Start development environment
./start-local.sh

# Or start services individually:
cd backend && ./run-local.sh      # Backend on :8081
cd frontend && ./run-local.sh     # Frontend on :3000
```

### First-Time Setup

```bash
# 1. Register admin account via API
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@miniups.com",
    "password": "Admin123!",
    "firstName": "Admin",
    "lastName": "User",
    "role": "ADMIN"
  }'

# 2. Login and get JWT token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin123!"
  }'

# 3. Use token in subsequent requests
# Add header: Authorization: Bearer <your-jwt-token>
```

## Core Modules

### 1. Authentication & Authorization

**Implementation:** JWT-based stateless authentication with RBAC

**User Roles:**
- `ADMIN` - Full system access, user management, ingestion control
- `OPERATOR` - Dispatch operations, fleet management, metrics viewing
- `DRIVER` - Delivery operations, truck assignment, status updates
- `USER` - Customer functions, package tracking, order creation

**Features:**
- BCrypt password hashing with secure salt generation
- JWT tokens with 24-hour expiration
- Refresh token mechanism
- OAuth2 integration (Google, OpenID Connect)
- Role-based endpoint protection
- Rate limiting per user role

**Endpoints:**
```
POST   /api/auth/register       # User registration
POST   /api/auth/login          # Login and get JWT
POST   /api/auth/refresh-token  # Refresh expired token
POST   /api/auth/logout         # Invalidate token
GET    /api/auth/me             # Get current user info
```

**File:** `backend/src/main/java/com/miniups/security/`

### 2. Shipment Management

**High-Performance Tracking IDs:**
- **Algorithm:** Leaf-Segment (Meituan)
- **Performance:** 50,000+ QPS
- **Format:** `UPS` + 14-digit timestamp + 4-digit sequence
- **Example:** `UPS202501151030450001`
- **Optimization:** Double-buffering with async pre-loading reduces DB queries by 1000x

**Shipment Lifecycle:**
1. `CREATED` - Order received from Amazon
2. `PICKED_UP` - Package collected from warehouse
3. `IN_TRANSIT` - En route to destination
4. `OUT_FOR_DELIVERY` - Final mile delivery
5. `DELIVERED` - Successfully delivered
6. `CANCELLED` - Order cancelled
7. `FAILED` - Delivery failed

**Features:**
- Real-time status updates via WebSocket
- Historical tracking with timestamps
- GPS coordinate tracking (X, Y)
- Weight and dimension recording
- Estimated vs. actual delivery times
- Package history audit trail

**Endpoints:**
```
GET    /api/shipments              # List shipments (paginated)
POST   /api/shipments              # Create new shipment
GET    /api/shipments/{id}         # Get shipment details
PUT    /api/shipments/{id}         # Update shipment
DELETE /api/shipments/{id}         # Cancel shipment
GET    /api/tracking/{trackingId}  # Track package by ID
GET    /api/tracking/{trackingId}/history  # Status history
```

**File:** `backend/src/main/java/com/miniups/service/ShipmentService.java`

### 3. Truck & Fleet Management

**Intelligent Dispatching:**
- Distance-based optimization algorithm
- Load balancing across available trucks
- Real-time truck status monitoring
- Capacity and utilization tracking
- GPS synchronization with World Simulator

**Truck Statuses:**
- `IDLE` - Available for assignment
- `EN_ROUTE_TO_PICKUP` - Heading to warehouse
- `LOADING` - Loading packages
- `DELIVERING` - On delivery route
- `RETURNING` - Returning to base

**Features:**
- Automatic truck assignment based on proximity
- Multi-package delivery routing
- Real-time location updates
- Driver assignment management
- Performance metrics tracking

**Endpoints:**
```
GET    /api/trucks                # List all trucks
GET    /api/trucks/{id}           # Get truck details
POST   /api/trucks                # Register new truck
PUT    /api/trucks/{id}/assign    # Assign delivery task
GET    /api/trucks/{id}/status    # Get real-time status
```

**File:** `backend/src/main/java/com/miniups/service/TruckManagementService.java`

### 4. Driver Management

**Driver Capabilities:**
- Profile and credentials management
- Delivery assignment tracking
- Performance metrics (rating, completed deliveries)
- Status updates (AVAILABLE, ON_DUTY, OFF_DUTY)

**Endpoints:**
```
GET    /api/drivers               # List drivers
GET    /api/drivers/{id}          # Get driver details
POST   /api/drivers               # Register driver
PUT    /api/drivers/{id}          # Update driver info
DELETE /api/drivers/{id}          # Remove driver
```

### 5. Real-Time Communication

**WebSocket Implementation:**
- **Protocol:** STOMP over WebSocket with SockJS fallback
- **Broker:** RabbitMQ STOMP relay
- **Heartbeat:** Ping every 30 seconds
- **Reconnection:** Automatic with exponential backoff

**Message Destinations:**
- `/topic/packages/{packageId}` - Package status broadcasts
- `/topic/trucks/{truckId}` - Truck location updates
- `/topic/drivers/{driverId}` - Driver notifications
- `/queue/user/{userId}` - Private user messages
- `/topic/admin/dashboard` - System-wide metrics

**Client Usage:**
```typescript
// Frontend example
import { socketService } from '@/services/socketService';

// Subscribe to package updates
socketService.subscribe(`/topic/packages/${packageId}`, (message) => {
  console.log('Package status:', message);
});

// Send status update
socketService.send('/app/package/status', {
  packageId: 'UPS202501151030450001',
  status: 'DELIVERED'
});
```

**File:** `backend/src/main/java/com/miniups/config/WebSocketConfig.java`

### 6. RAG-Powered AI Assistant

**Architecture:**

1. **Document Ingestion**
   - Automatic loading from `/knowledge/` directory
   - Chunking with configurable size (500-1000 chars)
   - Embedding generation via OpenRouter
   - Storage in PostgreSQL with pgvector

2. **Hybrid Retrieval**
   - **Semantic Search:** Vector similarity (default weight: 0.7)
   - **Keyword Search:** PostgreSQL full-text search (weight: 0.3)
   - Combined scoring with configurable weights
   - Top-K filtering with similarity threshold

3. **LLM Generation**
   - OpenRouter API integration
   - Context-grounded prompts with retrieved chunks
   - Citation support with source attribution
   - Confidence scoring

4. **Role-Aware Rate Limiting**
   - ADMIN: 100 queries/hour
   - OPERATOR: 50 queries/hour
   - DRIVER: 30 queries/hour
   - USER: 20 queries/hour

5. **Feedback Loop**
   - User feedback (POSITIVE/NEGATIVE)
   - Query logging with retrieval metrics
   - Performance analytics

**Configuration:**
```yaml
rag:
  enabled: true
  ingestion:
    enabled: true
    root-paths: knowledge
    schedule-enabled: false
    schedule-cron: "0 30 2 * * *"
  retrieval:
    top-k: 5
    similarity-threshold: 0.5
    semantic-weight: 0.7
    keyword-weight: 0.3
```

**Environment Variables:**
```bash
OPENROUTER_API_KEY=sk-or-your-api-key        # Required
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
RAG_ENABLED=true
RAG_INGESTION_ENABLED=true
```

**Endpoints:**
```
POST   /api/rag/query              # Query RAG system
POST   /api/rag/feedback           # Submit user feedback
POST   /api/rag/ingest             # Trigger ingestion (ADMIN)
GET    /api/rag/ingest/status      # Check ingestion status
```

**Metrics:**
- `rag.feedback.total` - Feedback counts by role
- `rag.retrieval.query_latency` - Retrieval performance
- `rag.retrieval.semantic_score` - Semantic match quality
- `rag.retrieval.keyword_score` - Keyword match quality
- `rag.retrieval.final_score` - Combined score distribution

**File:** `backend/src/main/java/com/miniups/rag/`

### 7. External System Integration

#### Amazon E-Commerce Platform

**Integration Type:** REST API Webhooks

**Incoming Webhooks:**
```
POST /api/webhooks/amazon/shipment-created      # New order
POST /api/webhooks/amazon/shipment-cancelled    # Order cancellation
POST /api/webhooks/amazon/address-changed       # Address update
```

**Outgoing Callbacks:**
```
POST {amazon-url}/shipment-loaded      # Pickup confirmation
POST {amazon-url}/status-update        # Status changes
POST {amazon-url}/delivered            # Delivery confirmation
```

**Security:** Webhook signature validation with shared secret

**File:** `backend/src/main/java/com/miniups/service/AmazonIntegrationService.java`

#### World Simulator Integration

**Integration Type:** TCP with Protocol Buffers

**Connection Details:**
- Protocol: TCP Binary Stream
- Port: 12345
- Serialization: Protocol Buffers 3.25
- Reconnection: Automatic with backoff

**Message Types:**
- `UConnect` - Initialize connection with world ID
- `UGoPickup` - Request truck to pickup location
- `UGoDeliver` - Request truck to delivery location
- `UQuery` - Query truck/package status
- World responses: `UFinished`, `UDeliveryMade`, `UTruckStatus`

**Features:**
- Real-time GPS tracking
- Warehouse synchronization
- Route planning and optimization
- Truck state management

**File:** `backend/src/main/java/com/miniups/service/WorldSimulatorService.java`

### 8. Event-Driven Messaging

**Transactional Outbox Pattern:**

Benefits:
- Guaranteed event delivery (at-least-once)
- Eliminates dual-write problems
- 70% reduction in DB write contention
- Atomic writes with business transactions

**Flow:**
1. Service writes event to `outbox_events` table in same transaction
2. `OutboxPollerService` polls for unpublished events
3. Events published to RabbitMQ/Kafka
4. Marked as published with timestamp
5. Consumers process events asynchronously

**Event Types:**
- `shipment.created` - New shipment created
- `shipment.status.changed` - Status update
- `truck.dispatched` - Truck assignment
- `user.registered` - New user signup
- `audit.log` - Audit trail entry

**Messaging Options:**

**RabbitMQ (Default):**
- AMQP 0.9.1 protocol
- STOMP for WebSocket relay
- Publisher confirms enabled
- Durable queues with persistence
- Dead letter queues for failures

**Kafka (Optional):**
- Event streaming alternative
- 3-6 partitions per topic
- Consumer groups for scaling
- Manual offset management
- Enable via: `messaging.kafka.enabled=true`

**File:** `backend/src/main/java/com/miniups/service/OutboxPollerService.java`

## API Documentation

### Interactive Documentation

- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **Knife4j:** http://localhost:8081/doc.html (Enhanced UI with search and export)
- **OpenAPI Spec:** http://localhost:8081/v3/api-docs

### Authentication

All protected endpoints require JWT token in header:
```
Authorization: Bearer <your-jwt-token>
```

### Core API Endpoints

#### Authentication
```bash
# Register new user
POST /api/auth/register
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"
}

# Login
POST /api/auth/login
{
  "username": "john_doe",
  "password": "SecurePass123!"
}

# Response includes JWT token
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}
```

#### Shipment Operations
```bash
# Create shipment
POST /api/shipments
{
  "originX": 100,
  "originY": 200,
  "destX": 500,
  "destY": 600,
  "weight": 5.5,
  "description": "Electronics package"
}

# Get shipment details
GET /api/shipments/{id}

# Track package
GET /api/tracking/UPS202501151030450001

# Response
{
  "trackingId": "UPS202501151030450001",
  "status": "IN_TRANSIT",
  "currentLocation": {"x": 300, "y": 400},
  "estimatedDelivery": "2025-01-16T14:00:00Z",
  "history": [...]
}
```

#### Truck Management
```bash
# List available trucks
GET /api/trucks?status=IDLE

# Assign truck to shipment
PUT /api/trucks/{truckId}/assign
{
  "shipmentId": 123,
  "action": "PICKUP"
}

# Get truck status
GET /api/trucks/{truckId}/status
```

#### Admin Operations
```bash
# List all users (ADMIN only)
GET /api/admin/users?page=0&size=20

# Get system metrics
GET /api/admin/metrics

# View audit logs
GET /api/admin/audit-logs?startDate=2025-01-01&endDate=2025-01-31
```

#### RAG Assistant
```bash
# Query AI assistant
POST /api/rag/query
{
  "query": "How do I track my package?",
  "context": {}
}

# Response
{
  "logId": "rag-20250115-001",
  "answer": "To track your package, use the tracking endpoint...",
  "confidence": 0.85,
  "sources": [
    {"title": "Tracking Guide", "score": 0.92},
    {"title": "API Documentation", "score": 0.78}
  ]
}

# Submit feedback
POST /api/rag/feedback
{
  "logId": "rag-20250115-001",
  "feedback": "POSITIVE",
  "comment": "Very helpful!"
}
```

### WebSocket Communication

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  console.log('Connected:', frame);

  // Subscribe to package updates
  stompClient.subscribe('/topic/packages/123', (message) => {
    const update = JSON.parse(message.body);
    console.log('Package update:', update);
  });

  // Subscribe to user-specific messages
  stompClient.subscribe('/queue/user/456', (message) => {
    console.log('Private message:', message.body);
  });
});
```

## Deployment

### Docker Compose Deployment

**Production Configuration:**
```bash
# Use production compose file
docker compose -f docker-compose.prod.yml up -d

# Scale backend instances
docker compose -f docker-compose.prod.yml up -d --scale ups-backend=3

# View logs
docker compose logs -f ups-backend

# Stop all services
docker compose down
```

### Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace mini-ups

# Apply configurations
kubectl apply -f frontend/k8s/deployment.yaml -n mini-ups
kubectl apply -f frontend/k8s/ingress.yaml -n mini-ups

# Check deployment status
kubectl get pods -n mini-ups
kubectl get services -n mini-ups

# View logs
kubectl logs -f deployment/ups-backend -n mini-ups

# Scale deployment
kubectl scale deployment ups-backend --replicas=5 -n mini-ups
```

**Resource Configuration:**
```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 500m
    memory: 1Gi
```

### AWS ECS Deployment

**Prerequisites:**
```bash
# Install AWS CLI
aws --version

# Configure credentials
aws configure

# Install ECS CLI
ecs-cli --version
```

**Deployment Steps:**
```bash
# Build and push Docker images
cd infrastructure/aws
./build-images.sh

# Create ECS cluster
aws ecs create-cluster --cluster-name mini-ups-prod

# Deploy infrastructure
./deploy.sh prod

# Check deployment status
aws ecs describe-services \
  --cluster mini-ups-prod \
  --services ups-backend ups-frontend

# View CloudWatch logs
aws logs tail /ecs/ups-backend --follow
```

**Infrastructure Components:**
- **VPC:** Private subnets across 2 AZs
- **RDS:** PostgreSQL 15 Multi-AZ
- **ElastiCache:** Redis cluster
- **ECS Fargate:** Serverless containers
- **ALB:** Application load balancer with SSL
- **CloudWatch:** Logs, metrics, and alarms
- **Secrets Manager:** Credential storage

### CI/CD Pipeline

**GitHub Actions Workflow:**

1. **Testing Stage**
   - Run unit tests
   - Run integration tests
   - Generate coverage reports
   - Validate code quality

2. **Build Stage**
   - Build backend (Maven)
   - Build frontend (Vite)
   - Create Docker images
   - Tag with commit SHA

3. **Security Stage**
   - Dependency vulnerability scan
   - Container image scanning
   - SAST analysis

4. **Deploy Stage**
   - Push images to ECR
   - Update ECS task definitions
   - Rolling update deployment
   - Run smoke tests

5. **Notification Stage**
   - Send deployment status
   - Update monitoring dashboards

**File:** `.github/workflows/ci-cd.yml`

**Manual Trigger:**
```bash
# Trigger deployment via GitHub CLI
gh workflow run ci-cd.yml \
  -f environment=production \
  -f version=v1.2.3
```

### Environment Configuration

**Development (`.env.dev`):**
```bash
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:postgresql://localhost:5432/ups_db
REDIS_HOST=localhost
LOG_LEVEL=DEBUG
```

**Production (`.env.prod`):**
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://rds-endpoint:5432/ups_db
REDIS_HOST=elasticache-endpoint
LOG_LEVEL=INFO
JWT_SECRET=${JWT_SECRET}  # From Secrets Manager
```

## Development

### Setup Development Environment

```bash
# Clone repository
git clone <repository-url>
cd mini-ups

# Backend setup
cd backend
./mvnw clean install

# Frontend setup
cd ../frontend
npm install

# Start development databases
docker compose up ups-database redis rabbitmq -d

# Start backend
cd backend
./mvnw spring-boot:run

# Start frontend (new terminal)
cd frontend
npm run dev
```

### Project Structure

```
mini-ups/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/miniups/
│   │   │   │   ├── config/           # Configuration classes
│   │   │   │   ├── controller/       # REST controllers
│   │   │   │   ├── service/          # Business logic
│   │   │   │   ├── repository/       # Data access
│   │   │   │   ├── model/            # Entity & DTO models
│   │   │   │   ├── security/         # Security configuration
│   │   │   │   ├── rag/              # RAG system modules
│   │   │   │   ├── exception/        # Exception handling
│   │   │   │   └── util/             # Utilities
│   │   │   └── resources/
│   │   │       ├── application.yml   # Main config
│   │   │       └── application-*.yml # Environment configs
│   │   └── test/                     # Test cases
│   ├── pom.xml                       # Maven dependencies
│   └── Dockerfile                    # Backend container
├── frontend/
│   ├── src/
│   │   ├── pages/                    # Page components
│   │   ├── components/               # Reusable components
│   │   ├── features/                 # Feature modules
│   │   ├── services/                 # API clients
│   │   ├── stores/                   # State management
│   │   ├── hooks/                    # Custom hooks
│   │   └── types/                    # TypeScript types
│   ├── package.json                  # Dependencies
│   └── Dockerfile                    # Frontend container
├── database/
│   └── init.sql                      # Database schema
├── knowledge/                        # RAG knowledge base
│   ├── user-guide/
│   ├── api-docs/
│   └── troubleshooting/
├── infrastructure/
│   ├── aws/                          # AWS CloudFormation
│   └── kubernetes/                   # K8s manifests
├── docker-compose.yml                # Local development
├── docker-compose.prod.yml           # Production config
└── README.md                         # This file
```

### Running Tests

**Backend Tests:**
```bash
cd backend

# Run all tests
./mvnw test

# Run with coverage
./mvnw verify

# Run specific test class
./mvnw test -Dtest=ShipmentServiceTest

# Run integration tests only
./mvnw verify -P integration-tests

# Generate coverage report (target/site/jacoco/index.html)
./mvnw jacoco:report
```

**Frontend Tests:**
```bash
cd frontend

# Run all tests
npm run test

# Run with coverage
npm run test:coverage

# Run in watch mode
npm run test:watch

# Run E2E tests
npm run test:e2e

# Coverage report: coverage/index.html
```

### Code Quality

**Backend:**
```bash
# Checkstyle
./mvnw checkstyle:check

# SpotBugs
./mvnw spotbugs:check

# Dependency check
./mvnw dependency-check:check
```

**Frontend:**
```bash
# ESLint
npm run lint

# Type check
npm run type-check

# Format code
npm run format
```

### Adding New Features

1. **Create Feature Branch**
```bash
git checkout -b feature/new-feature-name
```

2. **Backend Development**
   - Create entity in `model/entity/`
   - Create DTO in `model/dto/`
   - Create repository in `repository/`
   - Implement service in `service/`
   - Create controller in `controller/`
   - Add tests in `test/`

3. **Frontend Development**
   - Create components in `features/feature-name/`
   - Add API service in `services/`
   - Create store if needed in `stores/`
   - Add types in `types/`
   - Write tests

4. **Testing**
```bash
# Backend
./mvnw verify

# Frontend
npm run test
npm run type-check
```

5. **Commit and Push**
```bash
git add .
git commit -m "feat: add new feature"
git push origin feature/new-feature-name
```

6. **Create Pull Request**
   - Ensure tests pass
   - Add documentation
   - Request code review

### Development Tools

**Recommended IDE Extensions:**

**VSCode:**
- Extension Pack for Java
- Spring Boot Extension Pack
- ESLint
- Prettier
- Tailwind CSS IntelliSense
- TypeScript Vue Plugin (Volar)
- REST Client

**IntelliJ IDEA:**
- Spring Assistant
- MyBatis Plugin
- Protocol Buffer Editor
- Database Tools and SQL
- Docker Integration

### Database Management

**Access PostgreSQL:**
```bash
# Via Docker
docker exec -it ups-database psql -U postgres -d ups_db

# Direct connection
psql -h localhost -p 5431 -U postgres -d ups_db

# Common commands
\dt                    # List tables
\d shipments          # Describe table
\du                   # List users
\l                    # List databases
```

**Migrations:**
```bash
# Using Flyway
./mvnw flyway:migrate
./mvnw flyway:info
./mvnw flyway:validate
```

### Troubleshooting

**Common Issues:**

1. **Port Already in Use**
```bash
# Find process using port
lsof -i :8081

# Kill process
kill -9 <PID>
```

2. **Database Connection Failed**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check connection
psql -h localhost -p 5431 -U postgres
```

3. **Redis Connection Error**
```bash
# Check if Redis is running
docker ps | grep redis

# Test connection
redis-cli -h localhost -p 6380 -a test123 ping
```

4. **Frontend Build Errors**
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf .vite
```

5. **Backend Compilation Errors**
```bash
# Clean and rebuild
./mvnw clean install -DskipTests

# Clear local repository cache
rm -rf ~/.m2/repository/com/miniups
```

## Performance

### Benchmarks

**ID Generation:**
- **Throughput:** 50,000+ QPS
- **Latency:** < 1ms (p99)
- **Database Load:** 1000x reduction via batch pre-allocation
- **Implementation:** Leaf-Segment algorithm with double-buffering

**Write-Behind Cache:**
- **Contention Reduction:** 70% decrease in database write contention
- **Batch Size:** 100-1000 operations
- **Flush Interval:** 1 second (configurable)
- **Latency:** Sub-millisecond write response

**API Response Times (P95):**
- Simple queries: < 50ms
- Complex searches: 100-200ms
- Tracking lookup: < 30ms
- RAG queries: 1-3 seconds (includes LLM)

**Caching:**
- Cache hit rate: 85-95% (typical workload)
- Redis latency: < 5ms
- Caffeine latency: < 1ms

**Messaging:**
- RabbitMQ throughput: 1,000+ msg/sec
- Event processing: < 100ms (p95)
- Outbox polling: 1 second interval

### Optimization Tips

1. **Database Indexing**
   - Add indexes for frequently queried columns
   - Use composite indexes for multi-column queries
   - Analyze slow query logs

2. **Caching Strategy**
   - Cache frequently accessed data
   - Use appropriate TTL values
   - Implement cache warming

3. **Connection Pooling**
   - Configure HikariCP for optimal performance
   - Monitor connection usage
   - Adjust pool size based on load

4. **Async Processing**
   - Use `@Async` for non-blocking operations
   - Leverage messaging for background tasks
   - Implement circuit breakers

### Monitoring & Metrics

**Prometheus Endpoints:**
```
http://localhost:8081/actuator/prometheus
http://localhost:8081/actuator/metrics
```

**Key Metrics:**
- `http_server_requests_seconds` - API latency
- `jvm_memory_used_bytes` - Memory usage
- `system_cpu_usage` - CPU utilization
- `hikaricp_connections_active` - DB connections
- `cache_gets_total` - Cache operations
- `rag_retrieval_query_latency` - RAG performance

**Health Checks:**
```
GET /actuator/health
GET /actuator/health/db
GET /actuator/health/redis
```

## Contributing

We welcome contributions! Please follow these guidelines:

### Development Workflow

1. **Fork the repository**
2. **Create feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make changes** with clear commit messages
4. **Add tests** for new functionality
5. **Run tests** and ensure they pass
6. **Update documentation** if needed
7. **Push to branch** (`git push origin feature/amazing-feature`)
8. **Create Pull Request**

### Commit Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new RAG feedback mechanism
fix: resolve tracking ID generation race condition
docs: update API documentation
test: add integration tests for shipment service
refactor: improve truck assignment algorithm
perf: optimize database query performance
```

### Code Style

**Backend (Java):**
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use 4 spaces for indentation
- Max line length: 120 characters
- Use meaningful variable names

**Frontend (TypeScript):**
- Follow [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- Use 2 spaces for indentation
- Use TypeScript strict mode
- Prefer functional components

### Pull Request Checklist

- [ ] Code follows project style guidelines
- [ ] Tests added and passing
- [ ] Documentation updated
- [ ] No compiler warnings
- [ ] Commit messages follow convention
- [ ] Changes are backwards compatible
- [ ] Security implications considered

## License

```
MIT License

Copyright (c) 2024 Mini-UPS Development Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Support & Contact

- **Issues:** [GitHub Issues](https://github.com/your-repo/mini-ups/issues)
- **Discussions:** [GitHub Discussions](https://github.com/your-repo/mini-ups/discussions)
- **Documentation:** [Wiki](https://github.com/your-repo/mini-ups/wiki)

---

<div align="center">

**Built with ❤️ by the Mini-UPS Team**

[⭐ Star this project](https://github.com/your-repo/mini-ups) | [🍴 Fork it](https://github.com/your-repo/mini-ups/fork) | [📖 Documentation](https://docs.your-domain.com)

</div>
