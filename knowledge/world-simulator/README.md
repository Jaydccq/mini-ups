# World Simulator - Java Implementation

A high-performance Java implementation of the World Simulator component for the Mini-UPS distributed logistics system, built with Spring Boot and Netty.

## Overview

The World Simulator provides a real-time logistics environment simulation that coordinates warehouse operations, truck movements, and package deliveries between UPS and Amazon services. This Java version replaces the original C++ implementation while maintaining full protocol compatibility.

## Features

- **High-Performance Networking**: Netty-based TCP servers for UPS (port 12345) and Amazon (port 23456) connections
- **Real-Time Simulation**: Continuous truck movement simulation with configurable physics
- **Protocol Compatibility**: Full protobuf compatibility with existing UPS/Amazon clients  
- **Reliability**: ACK-based message acknowledgment with configurable retry mechanisms
- **Testing Support**: Configurable network flakiness injection (0-99%) for resilience testing
- **Spring Boot Integration**: Production-ready with metrics, health checks, and observability
- **Database Persistence**: PostgreSQL storage with Redis caching for high performance
- **Docker Ready**: Complete containerization with docker-compose integration

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL 15+ (for local development)
- Redis 7+ (for local development)

### Local Development

1. **Start the database services:**
   ```bash
   docker-compose up postgres redis -d
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Verify the simulator is running:**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

### Docker Deployment

1. **Start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Check service status:**
   ```bash
   docker-compose ps
   docker-compose logs world-simulator
   ```

## Configuration

### Key Configuration Properties

```yaml
world-sim:
  network:
    ups-port: 12345                    # TCP port for UPS connections
    amazon-port: 23456                 # TCP port for Amazon connections  
    flakiness: 0                       # Network flakiness (0-99%)
    ack-timeout-ms: 5000              # ACK timeout in milliseconds
    max-retries: 3                     # Maximum retry attempts
    
  simulation:
    tick-interval-ms: 100              # Simulation tick interval
    truck-speed-units-per-sec: 50      # Truck movement speed
    update-broadcast-interval-ms: 1000 # Client update frequency
    
  performance:
    database-batch-size: 100           # Database batch operations
    cache-ttl-seconds: 300             # Redis cache TTL
    metrics-enabled: true              # Enable Prometheus metrics
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `WORLD_SIM_FLAKINESS` | Network flakiness percentage (0-99) | `0` |
| `WORLD_SIM_UPS_PORT` | UPS TCP server port | `12345` |
| `WORLD_SIM_AMAZON_PORT` | Amazon TCP server port | `23456` |
| `POSTGRES_HOST` | PostgreSQL host | `localhost` |
| `POSTGRES_DB` | PostgreSQL database name | `worldSim` |
| `REDIS_HOST` | Redis host | `localhost` |

## Architecture

### System Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UPS Service   │    │ Amazon Service  │    │  Management UI  │
│    :8081        │    │     :8080       │    │     :8082       │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │TCP                   │TCP                   │HTTP
          │:12345                │:23456                │
          └─────────┬────────────┴──────────────────────┘
                    │
          ┌─────────▼──────────────────────────────────────────┐
          │              World Simulator                       │
          │  ┌─────────────────┐  ┌─────────────────┐        │
          │  │  Netty TCP      │  │  Spring Boot    │        │
          │  │  Servers        │  │  Application    │        │
          │  └─────────────────┘  └─────────────────┘        │
          │  ┌─────────────────┐  ┌─────────────────┐        │
          │  │  Simulation     │  │  Protocol       │        │
          │  │  Engine         │  │  Handlers       │        │
          │  └─────────────────┘  └─────────────────┘        │
          └─────────┬──────────────────────────────┬─────────┘
                    │                              │
          ┌─────────▼─────────┐          ┌─────────▼─────────┐
          │   PostgreSQL      │          │     Redis         │
          │   (worldSim)      │          │    (Cache)        │
          └───────────────────┘          └───────────────────┘
```

### Core Modules

- **Network Layer**: Netty-based TCP servers with custom protobuf codecs
- **Protocol Layer**: Message handling, ACK management, and flakiness injection
- **Simulation Engine**: Real-time truck movement and warehouse operations
- **Data Layer**: JPA entities with PostgreSQL persistence and Redis caching
- **Configuration**: Spring Boot configuration with environment-based profiles

## Testing

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests  
mvn verify

# All tests with coverage
mvn clean verify jacoco:report

# Performance tests
mvn test -Dtest=PerformanceTest
```

### Test Configuration

- **Unit Tests**: Mock-based testing with JUnit 5 and Mockito
- **Integration Tests**: Testcontainers for PostgreSQL and Redis
- **Performance Tests**: Load testing with simulated clients
- **Protocol Tests**: End-to-end testing with mock UPS/Amazon clients

## Monitoring and Observability

### Health Checks

```bash
# Application health
curl http://localhost:8082/actuator/health

# Component health details
curl http://localhost:8082/actuator/health/db
curl http://localhost:8082/actuator/health/redis
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8082/actuator/prometheus

# Application metrics
curl http://localhost:8082/actuator/metrics
```

### Key Metrics

- `world_sim_connections_active`: Number of active TCP connections
- `world_sim_messages_processed_total`: Total processed messages
- `world_sim_trucks_active`: Number of active trucks in simulation
- `world_sim_simulation_tick_duration`: Simulation tick processing time

## Development

### Project Structure

```
world-simulator/
├── src/main/java/com/miniups/worldsim/
│   ├── config/              # Spring configuration classes
│   ├── controller/          # REST controllers for management
│   ├── service/             # Business logic services
│   ├── repository/          # Data access repositories
│   ├── model/               # Entity and DTO classes
│   ├── network/             # Netty networking components
│   ├── simulation/          # Simulation engine components
│   └── util/                # Utility classes
├── src/main/proto/          # Protocol buffer definitions
├── src/main/resources/      # Configuration and migration files
└── src/test/                # Test classes
```

### Building from Source

```bash
# Clean build
mvn clean compile

# Generate protobuf classes
mvn protobuf:compile

# Package application
mvn clean package

# Skip tests for faster builds
mvn clean package -DskipTests
```

## Protocol Compatibility

The Java World Simulator maintains full compatibility with the original C++ implementation:

- **Message Format**: Identical protobuf schema (`world_ups.proto`)
- **TCP Framing**: Length-prefixed message framing
- **Sequence Numbers**: Compatible ACK-based reliability
- **Connection Handling**: Same connection lifecycle management
- **Simulation Behavior**: Identical physics and timing

## Deployment

### Docker Compose (Recommended)

```yaml
version: '3.8'
services:
  world-simulator:
    image: world-simulator:latest
    ports:
      - "12345:12345"
      - "23456:23456"
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - WORLD_SIM_FLAKINESS=0
    depends_on:
      - postgres
      - redis
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: world-simulator
spec:
  replicas: 1
  selector:
    matchLabels:
      app: world-simulator
  template:
    spec:
      containers:
      - name: world-simulator
        image: world-simulator:latest
        ports:
        - containerPort: 12345
        - containerPort: 23456
        - containerPort: 8082
```

## Troubleshooting

### Common Issues

1. **Port Conflicts**: Ensure ports 12345, 23456, and 8082 are available
2. **Database Connection**: Verify PostgreSQL is running and accessible
3. **Memory Issues**: Increase JVM heap size with `-Xmx1g`
4. **Connection Limits**: Check `max-connections` configuration

### Debug Logging

```yaml
logging:
  level:
    com.miniups.worldsim: DEBUG
    io.netty: INFO
```

### Performance Tuning

```yaml
world-sim:
  performance:
    database-batch-size: 200
    cache-ttl-seconds: 600
  simulation:
    tick-interval-ms: 50
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is part of the Mini-UPS distributed system implementation.

## Support

For issues and questions:
- Check the troubleshooting section
- Review application logs in `logs/world-simulator.log`
- Use health check endpoints for diagnostics
- Enable debug logging for detailed information