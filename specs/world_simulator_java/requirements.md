# World Simulator Java Version - Requirements Document

## Introduction

This document defines the requirements for implementing a Java version of the World Simulator component in the Mini-UPS distributed system. The World Simulator serves as the central physical environment simulation that coordinates warehouse operations, truck movements, and package deliveries for both UPS and Amazon services.

The Java implementation must provide identical functionality to the existing world_simulator_exec while being built on the Spring Boot ecosystem to maintain consistency with the existing Mini-UPS backend architecture.

## Requirements

### Requirement 1 - TCP Server Operations

**User Story:** As a distributed system architect, I need the World Simulator to accept TCP connections from both UPS and Amazon services so that it can coordinate logistics operations between the services.

#### Acceptance Criteria

1. When the World Simulator starts, the system shall bind to TCP port 12345 for UPS service connections.
2. When the World Simulator starts, the system shall bind to TCP port 23456 for Amazon service connections.
3. While multiple clients attempt to connect simultaneously, the system shall accept concurrent connections from multiple UPS and Amazon instances.
4. When a client disconnects unexpectedly, the system shall maintain state consistency and allow reconnection.
5. When the system shuts down gracefully, the system shall close all active connections with proper cleanup.

### Requirement 2 - Message Protocol Handling  

**User Story:** As a service developer, I need the World Simulator to handle protobuf message serialization and ACK-based reliability so that communications are robust and reliable.

#### Acceptance Criteria

1. When receiving protobuf messages, the system shall decode messages using the existing world_ups.proto schema.
2. When sending responses, the system shall encode responses using protobuf format with proper framing.
3. While processing messages with sequence numbers, the system shall implement ACK-based acknowledgment for message reliability.
4. When duplicate messages are received, the system shall detect and handle them idempotently using sequence numbers.
5. When ACK timeouts occur, the system shall implement configurable retry mechanisms with exponential backoff.

### Requirement 3 - Physical World Simulation

**User Story:** As a logistics coordinator, I need the World Simulator to maintain real-time truck positions and warehouse states so that accurate logistics operations can be performed.

#### Acceptance Criteria

1. When trucks are assigned to deliveries, the system shall calculate and update truck positions in real-time.
2. While trucks are traveling, the system shall simulate movement based on configurable speed parameters.
3. When trucks arrive at warehouses or destinations, the system shall trigger appropriate loading/unloading events.
4. When packages are loaded or unloaded, the system shall update warehouse inventory and truck cargo states.
5. While simulation is running, the system shall broadcast position updates to connected clients at configurable intervals.

### Requirement 4 - Database Integration

**User Story:** As a data manager, I need the World Simulator to persist simulation state in PostgreSQL so that operations are durable and consistent across restarts.

#### Acceptance Criteria

1. When the system starts, the system shall connect to PostgreSQL database using Spring Boot configuration.
2. While processing state changes, the system shall persist truck positions, warehouse states, and package locations.
3. When system restarts occur, the system shall restore simulation state from the database.
4. When concurrent updates happen, the system shall use proper transaction isolation to maintain data consistency.
5. While managing data, the system shall implement proper connection pooling and error handling.

### Requirement 5 - Flakiness Simulation

**User Story:** As a system tester, I need the World Simulator to inject configurable network flakiness so that I can test system resilience under unreliable conditions.

#### Acceptance Criteria

1. When flakiness parameter is set (0-99), the system shall randomly drop outbound messages with the specified probability percentage.
2. While flakiness is active, the system shall optionally delay or reorder messages to simulate network issues.  
3. When flakiness is set to 0, the system shall deliver all messages reliably without artificial failures.
4. When flakiness is configured, the system shall log dropped messages for debugging purposes.
5. While testing scenarios run, the system shall allow runtime flakiness adjustment without restart.

### Requirement 6 - Spring Boot Integration

**User Story:** As a DevOps engineer, I need the World Simulator to follow Spring Boot patterns so that it integrates seamlessly with existing Mini-UPS infrastructure.

#### Acceptance Criteria

1. When configuring the application, the system shall use Spring Boot @ConfigurationProperties for all settings.
2. While providing operational features, the system shall expose Actuator health checks and metrics endpoints.
3. When logging events, the system shall use SLF4J logging consistent with existing backend services.
4. When managing lifecycle, the system shall implement proper Spring Boot startup and shutdown hooks.
5. While handling errors, the system shall use Spring's global exception handling patterns.

### Requirement 7 - Event-Driven Architecture

**User Story:** As an application developer, I need the World Simulator to use Spring's event publishing mechanisms so that system components are loosely coupled and maintainable.

#### Acceptance Criteria

1. When simulation events occur, the system shall publish Spring ApplicationEvents for state changes.
2. While processing business logic, the system shall separate I/O handling from core simulation logic using event handlers.
3. When network events happen, the system shall delegate to appropriate service layer components via events.
4. When database operations complete, the system shall publish events for downstream processing.
5. While maintaining performance, the system shall use asynchronous event processing where appropriate.

### Requirement 8 - High Performance Requirements

**User Story:** As a system administrator, I need the World Simulator to handle high-throughput operations efficiently so that it can support large-scale logistics simulations.

#### Acceptance Criteria

1. When handling 1000+ concurrent connections, the system shall maintain response times under 100ms p95 latency.
2. While processing 10,000+ messages per second, the system shall use non-blocking I/O with Netty.
3. When managing 5,000+ trucks simultaneously, the system shall maintain smooth simulation performance.
4. While consuming memory, the system shall implement proper caching strategies to minimize database load.
5. When monitoring performance, the system shall expose Micrometer metrics for observability.

### Requirement 9 - Docker Integration

**User Story:** As a deployment engineer, I need the World Simulator to run in Docker containers so that it can be deployed consistently across environments.

#### Acceptance Criteria

1. When containerizing, the system shall build using multi-stage Docker with Java 17 base image.
2. While configuring for Docker, the system shall accept configuration via environment variables.
3. When deployed with docker-compose, the system shall integrate with existing PostgreSQL and Redis services.
4. While running in containers, the system shall expose proper health check endpoints.
5. When scaling horizontally, the system shall support multiple simulator instances with proper coordination.

### Requirement 10 - Testing and Quality Assurance

**User Story:** As a quality engineer, I need comprehensive test coverage for the World Simulator so that reliability and correctness are guaranteed.

#### Acceptance Criteria

1. When unit testing, the system shall achieve >90% code coverage with JUnit 5 and Mockito.
2. While integration testing, the system shall use Testcontainers for database and network testing.
3. When performance testing, the system shall validate throughput and latency requirements under load.
4. While testing flakiness, the system shall verify proper handling of message drops and network issues.
5. When building CI/CD pipelines, the system shall integrate with existing GitHub Actions workflows.