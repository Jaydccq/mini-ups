# CLAUDE.md

This file provides comprehensive guidance to Claude Code (claude.ai/code) when working with the Mini-UPS distributed system project.

## Project Overview

Mini-UPS is a complete distributed system simulating a UPS-like package delivery service. It consists of three interconnected services that handle e-commerce, shipping operations, and simulation through a world simulator environment.

**Project Status**: ✅ **FULLY IMPLEMENTED**
- Backend: Spring Boot with comprehensive entity model, security, and API
- Frontend: React with Radix UI, TypeScript, and real-time features
- Infrastructure: Docker containerization with unified configuration
- Documentation: Complete development guides and commented codebase

## Architecture

### Services Overview
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   世界模拟器      │    │   Amazon服务     │    │    UPS服务      │
│                 │    │                 │    │                 │
│ - 仓库管理       │◄──►│ - 电商平台       │◄──►│ - 运输管理       │
│ - 卡车调度       │    │ - 订单处理       │    │ - 包裹追踪       │
│ - 物流模拟       │    │ - 库存管理       │    │ - 用户系统       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
       │                       │                       │
       └───────────────────────┼───────────────────────┘
                              │
                    ┌─────────────────┐
                    │   projectnet    │
                    │   Docker网络     │
                    └─────────────────┘
```

### Technology Stack

#### **UPS Service** (✅ Complete Implementation)
- **Backend**: Spring Boot 3.2.0 + Java 17
- **Frontend**: React 18 + TypeScript + Vite + Radix UI
- **Database**: PostgreSQL 15 (`ups_db`)
- **Cache**: Redis 7
- **Security**: JWT Authentication + Spring Security
- **Communication**: REST APIs + WebSocket
- **Infrastructure**: Docker + Nginx

#### **Amazon Service** (External - Existing)
- **Backend**: Flask + Python 3.9
- **Database**: PostgreSQL (`mini_amazon`)
- **Communication**: HTTP REST APIs
- **Location**: `knowledge/amazon-ups/`

#### **World Simulator** (External - Existing)
- **Function**: Environment simulation for warehouses and trucks
- **Communication**: TCP sockets (port 12345 for UPS, 23456 for Amazon)
- **Integration**: Real-time position updates and truck management

## Development Commands

### 🚀 Quick Start (Recommended)

#### **Docker Mode** (All Services)
```bash
# Start complete Mini-UPS system
docker compose up --build

# Services will be available at:
# - UPS Frontend: http://localhost:3000
# - UPS Backend: http://localhost:8081
# - UPS Database: localhost:5431 (ups_db)
# - Redis Cache: localhost:6380
```

#### **Local Development Mode** (UPS Only)
```bash
# One-command startup (auto-checks dependencies)
./start-local.sh

# Or start services separately:
# Backend
cd backend && ./run-local.sh

# Frontend  
cd frontend && ./run-local.sh

# Local services:
# - Frontend: http://localhost:3000 (hot reload)
# - Backend: http://localhost:8081/api
# - Database: localhost:5432 (ups_db)
# - Redis: localhost:6380
```

### 🗄️ Database Management

#### **UPS Database Setup**
```bash
# Local development
createdb ups_db
# Credentials: postgres/abc123

# Docker (automatic)
# Database: ups_db
# Host: upsdb:5432
# Credentials: postgres/abc123
```

#### **Amazon Database** (External)
```bash
cd knowledge/amazon-ups/
python set_database.py

# Default admin credentials:
# Email: admin@example.com
# Password: admin
```

## Project Structure

### **UPS Service** (`/`) - 企业级Spring Boot架构

#### **🏗️ 完整项目结构**
```
📁 Mini-UPS Project Root
├── 📁 backend/                                    # 🚀 Spring Boot后端服务
│   ├── 📁 src/main/java/com/miniups/             # Java源代码主目录
│   │   ├── 🚀 MiniUpsApplication.java             # 应用程序启动类
│   │   │
│   │   ├── 📁 config/                             # 🔧 配置类层
│   │   │   ├── SecurityConfig.java               # JWT + Spring Security安全配置
│   │   │   ├── JpaConfig.java                   # 数据库连接池和JPA配置
│   │   │   ├── RedisConfig.java                 # Redis缓存配置和序列化
│   │   │   ├── WebSocketConfig.java             # WebSocket实时通信配置
│   │   │   ├── CorsConfig.java                  # 跨域资源共享配置
│   │   │   └── AsyncConfig.java                 # 异步任务处理配置
│   │   │
│   │   ├── 📁 controller/                         # 🎮 表现层 - REST API控制器
│   │   │   ├── AuthController.java               # 认证和授权API (/api/auth)
│   │   │   ├── UserController.java               # 用户管理API (/api/users)
│   │   │   ├── ShipmentController.java           # 订单管理API (/api/shipments)
│   │   │   ├── TruckController.java              # 车辆管理API (/api/trucks)
│   │   │   ├── TrackingController.java           # 包裹追踪API (/api/tracking)
│   │   │   ├── AmazonIntegrationController.java  # Amazon集成API (/api/amazon)
│   │   │   └── AdminController.java              # 管理员API (/api/admin)
│   │   │
│   │   ├── 📁 service/                            # 💼 业务逻辑层
│   │   │   ├── UserService.java                  # 用户管理 + 权限控制
│   │   │   ├── AuthService.java                  # 认证服务 + JWT令牌管理
│   │   │   ├── ShipmentService.java              # 订单业务 + 状态机管理
│   │   │   ├── TruckService.java                 # 车辆调度 + 路径优化算法
│   │   │   ├── PackageService.java               # 包裹管理 + 生命周期追踪
│   │   │   ├── NotificationService.java          # 消息通知 + 邮件短信发送
│   │   │   ├── WorldSimulatorService.java        # 外部系统集成服务
│   │   │   ├── AmazonIntegrationService.java     # Amazon平台集成服务
│   │   │   └── AnalyticsService.java             # 数据分析 + 报表生成
│   │   │
│   │   ├── 📁 repository/                         # 🗃️ 数据访问层
│   │   │   ├── UserRepository.java               # 用户数据访问 + 自定义查询
│   │   │   ├── ShipmentRepository.java           # 订单数据访问 + 复杂查询优化
│   │   │   ├── TruckRepository.java              # 车辆数据访问 + 位置查询
│   │   │   ├── PackageRepository.java            # 包裹数据访问 + 状态筛选
│   │   │   ├── ShipmentStatusHistoryRepository.java # 状态历史记录访问
│   │   │   └── AuditLogRepository.java           # 系统审计日志访问
│   │   │
│   │   ├── 📁 model/                              # 📊 数据模型层
│   │   │   ├── 📁 entity/                        # JPA实体映射类
│   │   │   │   ├── BaseEntity.java              # 基础实体类 (ID + 审计字段)
│   │   │   │   ├── User.java                    # 用户实体 + 角色权限关系
│   │   │   │   ├── Shipment.java                # 订单实体 + 复杂业务关系
│   │   │   │   ├── Package.java                 # 包裹实体 + 详细属性
│   │   │   │   ├── Truck.java                   # 车辆实体 + GPS位置信息
│   │   │   │   ├── ShipmentStatusHistory.java   # 状态变更历史记录
│   │   │   │   ├── Warehouse.java               # 仓库实体 + 容量管理
│   │   │   │   └── AuditLog.java                # 系统操作审计日志
│   │   │   │
│   │   │   ├── 📁 dto/                          # 数据传输对象
│   │   │   │   ├── 📁 user/                     # 用户相关DTO
│   │   │   │   │   ├── UserDto.java             # 用户信息DTO (安全字段过滤)
│   │   │   │   │   ├── CreateUserDto.java       # 创建用户请求DTO
│   │   │   │   │   ├── UpdateUserDto.java       # 更新用户信息DTO
│   │   │   │   │   └── UserProfileDto.java      # 用户档案DTO
│   │   │   │   ├── 📁 shipment/                 # 订单相关DTO
│   │   │   │   │   ├── ShipmentDto.java         # 订单信息DTO
│   │   │   │   │   ├── CreateShipmentDto.java   # 创建订单请求DTO
│   │   │   │   │   ├── UpdateShipmentDto.java   # 更新订单DTO
│   │   │   │   │   └── ShipmentTrackingDto.java # 订单追踪DTO
│   │   │   │   ├── 📁 auth/                     # 认证相关DTO
│   │   │   │   │   ├── LoginRequestDto.java     # 登录请求DTO
│   │   │   │   │   ├── LoginResponseDto.java    # 登录响应DTO
│   │   │   │   │   ├── RegisterRequestDto.java  # 注册请求DTO
│   │   │   │   │   └── JwtTokenDto.java         # JWT令牌DTO
│   │   │   │   └── 📁 response/                 # 统一响应DTO
│   │   │   │       ├── ApiResponseDto.java      # API标准响应格式
│   │   │   │       ├── PageResponseDto.java     # 分页响应格式
│   │   │   │       └── ErrorResponseDto.java    # 错误响应格式
│   │   │   │
│   │   │   └── 📁 enums/                        # 枚举类型定义
│   │   │       ├── UserRole.java                # 用户角色枚举
│   │   │       ├── ShipmentStatus.java          # 订单状态枚举
│   │   │       ├── TruckStatus.java             # 车辆状态枚举
│   │   │       ├── PackageType.java             # 包裹类型枚举
│   │   │       └── NotificationType.java        # 通知类型枚举
│   │   │
│   │   ├── 📁 security/                           # 🔒 安全组件层
│   │   │   ├── JwtTokenProvider.java             # JWT令牌生成和验证工具
│   │   │   ├── JwtAuthenticationFilter.java      # JWT认证过滤器
│   │   │   ├── CustomUserDetailsService.java     # Spring Security用户详情服务
│   │   │   ├── SecurityUtils.java                # 安全相关工具方法
│   │   │   ├── PasswordPolicy.java               # 密码策略验证器
│   │   │   └── RolePermissionEvaluator.java      # 角色权限评估器
│   │   │
│   │   ├── 📁 exception/                          # ❌ 异常处理层
│   │   │   ├── GlobalExceptionHandler.java       # 全局异常处理器
│   │   │   ├── BusinessException.java            # 业务异常基类
│   │   │   ├── UserNotFoundException.java        # 用户不存在异常
│   │   │   ├── ShipmentNotFoundException.java    # 订单不存在异常
│   │   │   ├── UnauthorizedException.java        # 未授权访问异常
│   │   │   ├── ValidationException.java          # 数据验证异常
│   │   │   └── ExternalServiceException.java     # 外部服务调用异常
│   │   │
│   │   ├── 📁 util/                               # 🛠️ 工具类层
│   │   │   ├── DateTimeUtils.java                # 日期时间处理工具
│   │   │   ├── ValidationUtils.java              # 数据验证工具
│   │   │   ├── EncryptionUtils.java              # 加密解密工具
│   │   │   ├── ResponseUtils.java                # 统一响应格式工具
│   │   │   ├── StringUtils.java                  # 字符串处理工具
│   │   │   ├── JsonUtils.java                    # JSON序列化工具
│   │   │   └── FileUtils.java                    # 文件操作工具
│   │   │
│   │   └── 📁 websocket/                          # 🌐 WebSocket实时通信
│   │       ├── WebSocketEventHandler.java        # WebSocket事件处理器
│   │       ├── ShipmentStatusWebSocketHandler.java # 订单状态推送处理器
│   │       └── NotificationWebSocketController.java # 实时通知控制器
│   │
│   ├── 📁 src/main/resources/                     # 资源文件目录
│   │   ├── application.yml                       # 主配置文件
│   │   ├── application-local.yml                 # 本地开发环境配置
│   │   ├── application-docker.yml                # Docker容器环境配置
│   │   ├── application-prod.yml                  # 生产环境配置
│   │   ├── 📁 db/migration/                      # 数据库迁移脚本
│   │   │   ├── V1__Initial_schema.sql            # 初始数据库结构
│   │   │   ├── V2__Add_indexes.sql               # 索引优化脚本
│   │   │   └── V3__Sample_data.sql               # 示例数据脚本
│   │   ├── 📁 static/                            # 静态资源文件
│   │   └── 📁 templates/                         # 模板文件 (邮件模板等)
│   │
│   ├── 📁 src/test/java/                          # 测试代码目录
│   │   ├── 📁 unit/                              # 单元测试
│   │   ├── 📁 integration/                       # 集成测试
│   │   └── 📁 e2e/                               # 端到端测试
│   │
│   ├── pom.xml                                   # Maven依赖管理文件
│   ├── Dockerfile                                # Docker容器构建文件
│   ├── .dockerignore                             # Docker忽略文件
│   └── run-local.sh                              # 本地运行脚本
│
├── 📁 frontend/                                   # 🎨 React前端应用
│   ├── 📁 src/
│   │   ├── 📁 components/                        # 🧩 React组件
│   │   │   ├── 📁 ui/                           # Radix UI基础组件
│   │   │   ├── 📁 layout/                       # 布局组件
│   │   │   ├── 📁 auth/                         # 认证相关组件
│   │   │   ├── 📁 shipment/                     # 订单管理组件
│   │   │   └── 📁 tracking/                     # 追踪界面组件
│   │   ├── 📁 pages/                            # 📄 页面组件
│   │   ├── 📁 hooks/                            # 🎣 自定义React Hooks
│   │   ├── 📁 services/                         # 🔌 API服务调用
│   │   ├── 📁 utils/                            # 🛠️ 前端工具函数
│   │   ├── main.tsx                             # 应用程序入口
│   │   └── App.tsx                              # 主应用组件
│   ├── package.json                             # Node.js依赖配置
│   ├── vite.config.ts                          # Vite构建配置
│   ├── tailwind.config.js                      # Tailwind CSS配置
│   ├── tsconfig.json                           # TypeScript配置
│   ├── Dockerfile                              # 前端容器构建
│   └── .env.local                              # 前端环境变量
│
├── 📁 docs/                                      # 📚 项目文档
│   ├── API_DOCUMENTATION.md                     # API接口文档
│   ├── DEPLOYMENT_GUIDE.md                      # 部署指南
│   ├── DEVELOPMENT_GUIDE.md                     # 开发指南
│   └── ARCHITECTURE_DESIGN.md                   # 架构设计文档
│
├── 📁 scripts/                                   # 🔧 自动化脚本
│   ├── start-local.sh                          # 本地开发启动脚本
│   ├── start-ups.sh                            # 完整系统启动脚本
│   ├── stop-ups.sh                             # 系统停止脚本
│   └── deploy.sh                               # 部署脚本
│
├── docker-compose.yml                           # 🐳 Docker编排配置
├── docker-compose.prod.yml                     # 生产环境Docker配置
├── .gitignore                                  # Git忽略文件配置
├── README.md                                   # 📖 项目说明文档
├── GUIDE.md                                    # 🏗️ 开发者详细指南
├── CLAUDE.md                                   # 🤖 AI助手指导文档
└── Spring_Boot_Tutorial_Guide.md              # 📚 Spring Boot学习教程
```

#### **🎯 分层架构设计原则**

**1. 📱 表现层 (Presentation Layer)**
- **职责**: HTTP请求处理、参数验证、响应格式化
- **组件**: @RestController、@RequestMapping、@Valid
- **特点**: 无业务逻辑、专注API设计、统一错误处理

**2. 💼 业务逻辑层 (Business Logic Layer)**
- **职责**: 核心业务规则、事务管理、业务流程编排
- **组件**: @Service、@Transactional、业务规则验证
- **特点**: 业务逻辑集中、事务边界清晰、可重用性高

**3. 🗃️ 数据访问层 (Data Access Layer)**
- **职责**: 数据持久化、查询优化、缓存管理
- **组件**: @Repository、JpaRepository、自定义查询
- **特点**: 数据访问抽象、查询性能优化、事务支持

**4. 📊 数据模型层 (Data Model Layer)**
- **职责**: 领域模型定义、数据传输对象、业务实体
- **组件**: @Entity、DTO、枚举类型、验证注解
- **特点**: 领域驱动设计、数据一致性、类型安全

**5. 🔧 配置层 (Configuration Layer)**
- **职责**: 应用配置、第三方集成、基础设施配置
- **组件**: @Configuration、@Bean、@Value、Profile
- **特点**: 配置集中管理、环境分离、依赖注入

#### **🏗️ 企业级架构特性**

**📈 可扩展性设计**
- 微服务友好的分层结构
- 接口与实现分离
- 依赖倒置原则应用
- 水平扩展支持

**🔒 安全性保障**  
- JWT无状态认证
- RBAC角色权限控制
- 数据传输加密
- SQL注入防护

**⚡ 性能优化**
- Redis多级缓存
- 数据库连接池
- JPA查询优化
- 异步处理支持

**🔧 运维友好**
- 多环境配置支持
- 健康检查端点
- 详细日志记录
- 监控指标集成

### **Amazon Service** (`knowledge/amazon-ups/`)
```
├── app/
│   ├── controllers/               # Flask routes
│   ├── services/                 # Business logic
│   ├── models/                   # Database models
│   └── templates/                # HTML templates
├── set_database.py               # Database setup
└── docker-compose.yml            # Amazon service config
```

## Configuration Details

### **Environment Variables**

#### **Docker Configuration**
```yaml
# PostgreSQL Database
POSTGRES_DB: ups_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: abc123
POSTGRES_HOST: upsdb
POSTGRES_PORT: 5432

# Redis Cache
REDIS_HOST: redis
REDIS_PORT: 6380

# External Integrations
WORLD_HOST: host.docker.internal
WORLD_PORT: 12345
AMAZON_API_URL: http://host.docker.internal:8080
```

#### **Local Development**
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/ups_db
DB_USERNAME=postgres
DB_PASSWORD=abc123

# Cache
REDIS_HOST=localhost
REDIS_PORT=6380

# Profile
SPRING_PROFILES_ACTIVE=local
```

### **Network Configuration**
- **Docker Network**: `projectnet` (external)
- **Port Mappings**:
  - Frontend: `3000:80`
  - Backend API: `8081:8081` (via nginx)
  - Database: `5431:5432`
  - Redis: `6380:6379`

### **Security Configuration**
- **Authentication**: JWT tokens with HS256 signing
- **Password Encryption**: BCrypt
- **Session Management**: Stateless (JWT-based)
- **CORS**: Configured for frontend integration
- **Role-based Access**: USER/ADMIN/DRIVER/OPERATOR

## Development Workflow

### **Code Quality Standards**
✅ **All files have comprehensive documentation comments**
- Purpose and functionality explanation
- Technical implementation details
- Integration points and dependencies
- Usage examples and best practices

### **Database Design**
- **Entities**: User, Shipment, Truck, Package, etc.
- **Relationships**: Properly mapped with JPA annotations
- **Indexing**: Optimized for common queries
- **Audit Trail**: BaseEntity with timestamps and versioning

### **API Design**
- **RESTful**: Standard HTTP methods and status codes
- **Documentation**: Swagger/OpenAPI integration
- **Validation**: Bean Validation with custom constraints
- **Error Handling**: Global exception handling with structured responses

### **Testing Strategy**
- **Unit Tests**: Service layer with Mockito
- **Integration Tests**: Repository and controller testing
- **Database Tests**: H2 in-memory for fast testing
- **API Tests**: RESTAssured for endpoint testing

## Service Integration

### **UPS ↔ Amazon Communication**
```
Amazon System                    UPS System
     │                              │
     ├── POST /api/shipments        │
     │   (create shipping order)    │
     │                              ├── Truck Assignment
     │                              ├── Route Planning
     │                              └── Status Updates
     │                              │
     │   ← Webhook Notifications ←  │
     │     (status changes)         │
```

### **UPS ↔ World Simulator**
```
UPS System                    World Simulator
     │                              │
     ├── TCP Connection :12345      │
     │   (truck positions)          │
     │                              ├── Truck Movement
     │                              ├── Warehouse Events
     │                              └── Delivery Updates
     │                              │
     │   ← Real-time Updates ←      │
     │     (position data)          │
```

## AWS Deployment Strategy

### **⚠️ Cost Analysis for Personal Projects**

**Current Architecture Assessment:**
- Your VPC setup with NAT Gateway incurs **~$32/month** base cost ($0.045/hour)
- Plus $0.045/GB data processing fees
- RDS + ElastiCache would add $50-100+/month minimum
- **Total estimated cost: $80-150/month** for personal learning project

**Recommended Cost-Optimized Architecture:**

#### **Option A: Simplified VPC (Recommended for Learning)**
```
┌─────────────────────────────────────────────────────────────┐
│                        VPC (10.0.0.0/16)                   │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Public Subnet (10.0.0.0/20)               ││
│  │                                                         ││
│  │  ┌─────────────────────────────────────────────────────┐││
│  │  │        EC2 Instance (t4g.micro)                     │││
│  │  │                                                     │││
│  │  │  ┌─────────────────────────────────────────────────┐│││
│  │  │  │            Docker Compose                       ││││
│  │  │  │                                                 ││││
│  │  │  │  • Spring Boot App                              ││││
│  │  │  │  • PostgreSQL Container                         ││││
│  │  │  │  • Redis Container                              ││││
│  │  │  │  • React Frontend (Nginx)                       ││││
│  │  │  └─────────────────────────────────────────────────┘│││
│  │  └─────────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────────┘│
│           │                                                 │
│           └─────────────► Internet Gateway                  │
└─────────────────────────────────────────────────────────────┘
```

**Benefits:**
- **Cost**: $0-6/month (Free Tier eligible for 12 months)
- **Learning**: Full AWS networking knowledge
- **Security**: Your existing security groups provide excellent protection
- **Flexibility**: Complete control over configuration

**Implementation Steps:**
1. **Remove NAT Gateway** (saves $32/month immediately)
2. **Delete private subnets** (simplify architecture)
3. **Launch EC2 in public subnet** with your security groups
4. **Use Docker Compose** for application stack

#### **Option B: AWS Lightsail (Simple & Predictable)**
- **Cost**: $5/month fixed (includes 1GB RAM, 1 vCPU, 40GB SSD, 2TB transfer)
- **Effort**: Minimal setup, immediate deployment
- **Learning**: Application deployment, less AWS infrastructure knowledge

### **Production Deployment Strategy**

When ready for production scaling, consider:
- **ECS Fargate** for container orchestration
- **RDS** for managed database with backups
- **ElastiCache** for managed Redis
- **CloudFront** for CDN and SSL termination
- **Route 53** for DNS management

## Deployment and Operations

### **Health Checks**
```bash
# Application health
curl http://localhost:8081/api/health

# Database connectivity
docker exec mini-ups-postgres pg_isready -U postgres

# Redis connectivity  
docker exec mini-ups-redis redis-cli ping
```

### **Logging and Monitoring**
- **Application Logs**: `/logs/mini-ups.log`
- **Database Logs**: Docker container logs
- **Performance Metrics**: Spring Boot Actuator endpoints
- **Real-time Monitoring**: WebSocket connection status

### **Backup and Recovery**
```bash
# Database backup
docker exec mini-ups-postgres pg_dump -U postgres ups_db > backup.sql

# Database restore
docker exec -i mini-ups-postgres psql -U postgres ups_db < backup.sql
```

## Troubleshooting

### **Common Issues**

#### **Docker Network Problems**
```bash
# Ensure projectnet exists
docker network create projectnet

# Check network connectivity
docker network inspect projectnet
```

#### **Database Connection Issues**
```bash
# Check database status
docker compose logs upsdb

# Test connection
psql -h localhost -p 5431 -U postgres -d ups_db
```

#### **Frontend Build Problems**
```bash
# Clean install
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### **Port Conflicts**
If ports are already in use:
```bash
# Check port usage
lsof -i :5431  # Database
lsof -i :6380  # Redis
lsof -i :8081  # Backend
lsof -i :3000  # Frontend
```

## Development Best Practices

### **Code Standards**
- **Comments**: Every class has comprehensive header documentation
- **Naming**: Clear, descriptive names following Java conventions
- **Architecture**: Layered architecture with clear separation of concerns
- **Error Handling**: Comprehensive exception handling with meaningful messages

### **Git Workflow**
- **Branches**: Feature branches from main
- **Commits**: Descriptive commit messages with context
- **Reviews**: Code review for all changes
- **Documentation**: Update CLAUDE.md for significant changes

### **Performance Considerations**
- **Database**: Indexed queries and connection pooling
- **Caching**: Redis for session data and frequent queries
- **Frontend**: Code splitting and lazy loading
- **API**: Pagination for large datasets

---

## Quick Reference

### **Default Credentials**
- **UPS Database**: `postgres/abc123`
- **Amazon Admin**: `admin@example.com/admin`

### **Key URLs**
- **UPS Frontend**: http://localhost:3000
- **UPS Backend**: http://localhost:8081/api
- **API Documentation**: http://localhost:8081/swagger-ui.html
- **Amazon System**: http://localhost:8080

### **Essential Commands**
```bash
# Start everything
docker compose up --build

# Local development
./start-local.sh

# View logs
docker compose logs -f

# Stop services
docker compose down
```

This project represents a complete, production-ready distributed system with comprehensive documentation, testing, and deployment strategies. All components are fully implemented and documented for easy maintenance and extension.