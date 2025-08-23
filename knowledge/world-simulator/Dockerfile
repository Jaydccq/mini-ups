# World Simulator Docker Configuration
# Multi-stage build for optimized production image

# Stage 1: Build stage with Maven and JDK
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy Maven configuration files
COPY pom.xml .
COPY src/main/proto src/main/proto

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage with JRE
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S worldsim && \
    adduser -S worldsim -u 1001 -G worldsim

# Install necessary packages
RUN apk add --no-cache \
    curl \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Copy the JAR file from builder stage
COPY --from=builder /app/target/world-simulator-*.jar app.jar

# Copy configuration files
COPY --from=builder /app/src/main/resources/application*.yml ./

# Change ownership to non-root user
RUN chown -R worldsim:worldsim /app

# Switch to non-root user
USER worldsim

# Create logs directory
RUN mkdir -p logs

# Expose ports
EXPOSE 12345 23456 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UnlockExperimentalVMOptions \
               -XX:+UseCGroupMemoryLimitForHeap \
               -Djava.security.egd=file:/dev/./urandom"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]