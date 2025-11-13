# 🚚 Mini-UPS: Enterprise Distributed Logistics System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![AWS](https://img.shields.io/badge/AWS-Cloud%20Native-orange.svg)](https://aws.amazon.com/)

> 🏗️ **Production-Ready Enterprise Distributed Logistics Management System**  
> Built with microservices architecture, supporting Protocol Buffer high-performance communication, real-time tracking, intelligent scheduling, with complete CI/CD pipeline and AWS cloud-native deployment capabilities.

## ✨ Key Features

- **Microservices Architecture**: Spring Boot 3.4 + React 18 modern stack
- **JWT Authentication & RBAC**: Secure access control
- **High-performance Communication**: Protocol Buffer binary streaming
- **Intelligent Scheduling**: Optimized truck dispatching algorithms
- **Real-time Tracking**: WebSocket status updates
- **Cloud Native Deployment**: Docker + AWS ECS Fargate
- **CI/CD Automation**: GitHub Actions pipeline
- **Comprehensive Monitoring**: CloudWatch metrics and alerts

## 🏛️ System Architecture

### 🎯 Layered Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Mini-UPS Enterprise Distributed System           │
├─────────────────────────────────────────────────────────────────────┤
│  📱 Presentation Layer (React)                                      │
│     React 18 + TypeScript + Radix UI + Tailwind CSS                 │
├─────────────────────────────────────────────────────────────────────┤
│  🌐 Gateway Layer                                                   │
│     Nginx + ALB + SSL Termination + Load Balancing                  │
├─────────────────────────────────────────────────────────────────────┤
│  🚀 Application Layer (Spring Boot)                                 │
│     ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐     │
│     │  🎮 Controller  │ │  💼 Service     │ │  🔒 Security    │     │
│     │  @RestController│ │  @Service       │ │  Spring Security│     │
│     └─────────────────┘ └─────────────────┘ └─────────────────┘     │
├─────────────────────────────────────────────────────────────────────┤
│  📊 Data Access Layer                                               │
│     Spring Data JPA + Repository Pattern                            │
├─────────────────────────────────────────────────────────────────────┤
│  🗄️ Data Layer                                                     │
│     PostgreSQL 15 (Primary) + Redis 7 (Cache)                       │
├─────────────────────────────────────────────────────────────────────┤
│  🔗 Integration Layer                                               │
│     Protocol Buffer + TCP Socket + REST API + WebSocket             │
└─────────────────────────────────────────────────────────────────────┘
```

### 🌐 Distributed Communication

```
    ┌─────────────────────────────────────────────────────────────┐
    │                      External System Integration            │
    └─────────────────────────────────────────────────────────────┘
              │                    │                    │
    ┌─────────▼─────────┐ ┌────────▼────────┐ ┌────────▼────────┐
    │  World Simulator  │ │   UPS Core      │ │  Amazon         │
    │  (TCP:12345)      │ │ (Spring Boot)   │ │  (Flask API)    │
    │                   │ │                 │ │                 │
    │ 🌍 Environment    │ │ 🚚 Logistics    │ │ 🛒 Order        │
    │ • GPS Tracking    │ │ • Scheduling    │ │ • Order         │
    │ • Truck Sync      │◄┤ • Package       │◄┤ • Status        │
    │ • Warehouse Mgmt  │ │ • Real-time     │ │ • Address       │
    │ • Route Planning  │ │ • User Mgmt     │ │ • Callbacks     │
    │                   │ │                 │ │                 │
    │ Protocol Buffer   │ │ REST API        │ │ REST + Webhook  │
    │ Binary Stream     │ │ JSON/HTTP       │ │ JSON/HTTP       │
    └───────────────────┘ └─────────────────┘ └─────────────────┘
```

## 🚀 Quick Start

### 🐳 Docker Deployment (Recommended)

```bash
# 1. Clone the project
git clone <repository-url>
cd mini-ups

# 2. Start the complete system
docker compose up --build

# 🌟 That's it! All services will start up within minutes
```

**🎯 Access Addresses:**
| Service | Address | Description |
|------|------|------|
| 🖥️ Frontend | http://localhost:3000 | Modern React UI |
| 🔌 Backend API | http://localhost:8081 | Spring Boot REST API |
| 📚 API Docs | http://localhost:8081/swagger-ui.html | Swagger UI |
| 🛒 Amazon System | http://localhost:8080 | E-commerce Interface |
| 🗄️ Database | localhost:5431 | PostgreSQL (postgres/abc123) |
| ⚡ Redis Cache | localhost:6380 | High-performance Cache |

### 💻 Local Development

```bash
# 🛠️ Prerequisites Check
./start-local.sh --check

# 🚀 One-click Start Local Development Environment
./start-local.sh

# 🔄 Start Services Separately
cd backend && ./run-local.sh    # Backend Development Server
cd frontend && ./run-local.sh   # Frontend Development Server
```

## 🔧 Technology Stack

### 🗄️ Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.4.1 | Microservices Framework |
| Spring Security | 6.4.3 | JWT Authentication |
| PostgreSQL | 15 | Relational Database |
| Redis | 7 | Cache & Session Storage |
| RabbitMQ | 3.x | Task queues & WebSocket bridge |
| Apache Kafka | 3.x | Event streaming & analytics fan-out |
| Protocol Buffers | 4.29.2 | Binary Serialization |
| Netty | 4.1.117.Final | TCP Client |

### 🎨 Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.2 | UI Framework |
| TypeScript | 5.2 | Type-safe JavaScript |
| Vite | 7.0.5 | Build Tool + HMR |
| Radix UI | 1.0 | Accessible Components |
| Tailwind CSS | 3.4.1 | Utility-first CSS |

### ☁️ Infrastructure
| Technology | Purpose |
|------------|---------|
| Docker | Containerization |
| AWS ECS Fargate | Serverless Containers |
| GitHub Actions | CI/CD Pipeline |
| CloudFormation | Infrastructure as Code |

### 🔄 Messaging

- **Transactional Outbox → RabbitMQ**: Reliable command processing and WebSocket fan-out (default).
- **Transactional Outbox → Apache Kafka**: Optional event streaming pipeline for analytics, CDC, and downstream services. Enable via `messaging.kafka.enabled=true` and configure bootstrap servers in `application.yml`.

## 📋 Core Modules

- **Authentication**: JWT + RBAC (Customer/Driver/Admin)
- **Order Management**: Full lifecycle tracking
- **Truck Scheduling**: Distance-based optimization
- **Real-time Tracking**: GPS + Status updates
- **External Integration**: World Simulator + Amazon API
- **Monitoring**: CloudWatch + Health checks

## 🧠 RAG Assistant (Phase 1)

- **Context-aware guidance**: New Mini-UPS助手提供知识检索 + 大模型生成的组合答案，并附带引用
- **Role-aware answers**: 自动读取当前登录用户角色（管理员/调度/司机）调整响应语气与速率限制
- **混合检索**: 语义向量 + PostgreSQL 全文检索双路召回，支持权重调整
- **知识库摄取**: 后端启动时自动从 `knowledge/` 目录和文档中分块、生成向量并写入 pgvector
- **前端体验**: 登录后右下角浮动图标即可打开聊天窗口，实时查看回答与参考文档
- **摄取治理**: 管理员可手动触发重建，并查看最近一次摄取作业的执行状态
- **用户反馈闭环**: 聊天界面支持点赞/点踩，反馈会写入日志并暴露指标
  - `rag.feedback.total`：按角色统计正/负反馈数量
  - `rag.retrieval.query_latency`：区分成功/空/错误的检索耗时
  - `rag.retrieval.semantic_score` / `keyword_score` / `final_score`：检索得分分布
  - `rag.retrieval.weight_dominant`：语义或关键词贡献占主导的次数

### 🔐 配置步骤

1. **升级数据库镜像**：`docker-compose*.yml` 已改用 `ankane/pgvector:pg15`，确保 Postgres 自带 `pgvector` 扩展
2. **提供 OpenAI 凭证**：在后端环境变量中设置

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `OPENROUTER_API_KEY` | OpenRouter 密钥（用于嵌入 + 回答） | _必填_ |
| `OPENROUTER_BASE_URL` | OpenRouter API 地址 | `https://openrouter.ai/api/v1` |
| `OPENROUTER_SITE_URL` | 项目对外访问 URL（OpenRouter 统计用） | `http://localhost` |
| `OPENROUTER_APP_NAME` | 应用名称（展示在 OpenRouter 控制台） | `Mini-UPS RAG` |
| `OPENAI_API_KEY` | （可选）备用 OpenAI 密钥 | _(空)_ |
| `OPENAI_API_BASE_URL` | （可选）OpenAI API 地址 | `https://api.openai.com/v1` |
| `RAG_ENABLED` | 总开关 | `true` |
| `RAG_INGESTION_ENABLED` | 启动时是否自动摄取知识库 | `true` |
| `RAG_INGESTION_ROOT_PATHS` | 逗号分隔的本地知识库目录 | `knowledge` |
| `RAG_INGESTION_SCHEDULE_ENABLED` | 是否启用定时摄取 | `false` |
| `RAG_INGESTION_SCHEDULE_CRON` | 定时任务 Cron 表达式 | `0 30 2 * * *` |
| `RAG_RETRIEVAL_SEMANTIC_WEIGHT` | 语义检索权重 (0~1) | `0.7` |
| `RAG_RETRIEVAL_KEYWORD_WEIGHT` | 关键词检索权重 (0~1) | `0.3` |

> ❗ 未配置 `OPENAI_API_KEY` 时，系统会跳过向量生成/回答，但仍会正常启动。

### 🚀 使用建议

- 本地开发：
  ```bash
  export OPENROUTER_API_KEY=sk-or-...
  docker compose up ups-database -d  # pgvector 支持
  ./start-local.sh                   # 启动全栈
  ```
- 需要重新摄取文档时，更新 `knowledge/` 内容后重启后端即可
- 当前回答为快速原型，推荐在 Phase 2 继续完善重排序、缓存与准入控制

## 🔗 API Endpoints

### 🔑 Authentication
```bash
POST /api/auth/login          # User Login
POST /api/auth/register       # User Registration
POST /api/auth/refresh-token  # Refresh Token
```

### 📦 Orders
```bash
GET    /api/shipments         # List Orders
POST   /api/shipments         # Create Order
GET    /api/shipments/{id}    # Order Details
PUT    /api/shipments/{id}    # Update Order
DELETE /api/shipments/{id}    # Cancel Order
```

### 🚛 Trucks
```bash
GET  /api/trucks              # List Trucks
GET  /api/trucks/{id}/status  # Truck Status
PUT  /api/trucks/{id}/assign  # Assign Task
```

### 📍 Tracking
```bash
GET /api/tracking/{trackingNumber}         # Package Tracking
GET /api/tracking/{trackingNumber}/history # Status History
```

**📚 Complete API Documentation**: http://localhost:8081/swagger-ui.html

## 🚢 Deployment

### ☁️ AWS Cloud Deployment

```bash
# 🔧 Configure AWS Credentials
aws configure

# 🚀 Deploy to Production
cd infrastructure/aws
./deploy.sh prod

# 📊 View Deployment Status
aws cloudformation describe-stacks --stack-name mini-ups-infrastructure-prod
```

### 🔄 CI/CD Pipeline

GitHub Actions automates:
1. **🧪 Testing**: Unit + Integration tests
2. **🔒 Security**: Vulnerability scanning
3. **🐳 Build**: Multi-stage Docker images
4. **☁️ Deploy**: ECR + ECS update
5. **✅ Verify**: Health checks

## 🛠️ Development

### 📋 Requirements

```bash
Java 17+                    # OpenJDK Recommended
Node.js 18+                 # Frontend Development
Maven 3.9+                  # Build Tool
PostgreSQL 15+              # Database
Redis 7+                    # Cache
Docker & Docker Compose     # Containerization
```

### 🧪 Testing

```bash
# Backend Tests
cd backend
./mvnw clean test                    # Unit Tests
./mvnw verify                        # Integration Tests

# Frontend Tests
cd frontend
npm run test                         # Unit Tests
npm run test:coverage                # Coverage Report
```

## 📄 License

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

<div align="center">

**🚀 Mini-UPS - Redefining Enterprise Logistics Management**

[![⭐ Give a Star](https://img.shields.io/github/stars/your-repo/mini-ups?style=social)](https://github.com/your-repo/mini-ups)
[![🍴 Fork Project](https://img.shields.io/github/forks/your-repo/mini-ups?style=social)](https://github.com/your-repo/mini-ups/fork)

[🏠 Project Homepage](https://your-domain.com) | [📖 Documentation](https://docs.your-domain.com) | [🎮 Demo](https://demo.your-domain.com)

</div>
- 手动重建知识库（管理员）
  ```bash
  # 触发摄取 (需 ADMIN 角色)
  POST /api/rag/ingest

  # 查看最近一次摄取状态 (ADMIN / OPERATOR)
  GET /api/rag/ingest/status
  ```

- 反馈采集
  ```bash
  # 发送点赞或点踩 (需登录用户)
  POST /api/rag/feedback
  {
    "logId": "<响应中返回的 logId>",
    "feedback": "POSITIVE" | "NEGATIVE",
    "comment": "(可选)"
  }
  ```
