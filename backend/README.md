# Mini-UPS Backend

Spring Boot-based backend service for the Mini-UPS distributed package delivery system.

## Architecture

This backend follows a clean layered architecture pattern:

- **Controllers**: RESTful API endpoints with comprehensive validation
- **Services**: Business logic implementation and transaction management  
- **Repositories**: Data access layer with JPA/Hibernate
- **Security**: JWT-based authentication with role-based access control
- **Configuration**: Modular configuration for different environments

## Key Features

### 🚀 High-Performance Architecture
- **Connection Pooling**: Apache HttpClient5 for external service communication
- **Caching**: Redis integration for session management and frequent queries
- **Async Processing**: RabbitMQ for decoupled background tasks
- **Monitoring**: Comprehensive metrics with Micrometer/Prometheus

### 🔐 Security
- JWT authentication with configurable expiration
- BCrypt password encryption
- Role-based access control (USER/ADMIN/DRIVER/OPERATOR)
- CORS configuration for frontend integration

### 📊 Data Management
- PostgreSQL primary database with connection pooling
- JPA/Hibernate with optimized query patterns
- Database migration support with Flyway-compatible structure
- Audit trail with BaseEntity timestamps

### 🔄 Integration Capabilities
- **World Simulator**: TCP socket communication for truck tracking
- **Amazon Service**: REST API integration for order management
- **WebSocket**: Real-time status updates for frontend

## Quick Start

### Local Development
```bash
# Start with all dependencies
./run-local.sh

# Or manually:
# 1. Ensure PostgreSQL and Redis are running
# 2. Set environment variables (see Configuration section)
# 3. Run: mvn spring-boot:run
```

### Docker Deployment
```bash
# From project root
docker compose up --build
```

## Configuration

### Required Environment Variables
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/ups_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=abc123

# Security
JWT_SECRET=your-256-bit-secret-key-here

# External Services
WORLD_SIMULATOR_HOST=localhost
AMAZON_API_BASE_URL=http://localhost:8080
```

### Optional Configuration
```bash
# HTTP Client Tuning
HTTP_CLIENT_MAX_TOTAL_CONNECTIONS=100
HTTP_CLIENT_MAX_DEFAULT_PER_ROUTE=20

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6380

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
```

## API Documentation

When running, visit:
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **API Docs**: http://localhost:8081/api-docs

## Health Monitoring

- **Health Check**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/metrics
- **Prometheus**: http://localhost:8081/actuator/prometheus

## Testing

```bash
# Unit tests only
mvn test

# Integration tests (requires TestContainers)
mvn verify

# With coverage
mvn clean test jacoco:report
```

## Project Structure

```
src/main/java/com/miniups/
├── config/           # Configuration classes
├── controller/       # REST API controllers
├── service/          # Business logic layer
├── repository/       # Data access layer
├── model/
│   ├── entity/       # JPA entities
│   ├── dto/          # Data transfer objects
│   └── enums/        # Type definitions
├── security/         # Authentication & authorization
├── exception/        # Exception handling
└── util/             # Utility classes
```

## Performance Notes

- Uses connection pooling for HTTP clients (prevents connection exhaustion)
- Redis caching for frequently accessed data
- JPA query optimization with proper indexing
- Async processing for resource-intensive operations

## Contributing

1. Follow the existing code structure and naming conventions
2. Add comprehensive JavaDoc comments for public APIs
3. Include unit tests for new functionality
4. Update this README for significant architectural changes

For detailed development guidelines, see `CLAUDE.md` in the project root.