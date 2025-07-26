# Multi-stage build for Spring Boot application
FROM --platform=linux/amd64 maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy proto files and generate protobuf classes
COPY src/main/proto ./src/main/proto
RUN mvn protobuf:compile

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.gitcommitid.skip=true

# Runtime stage with JRE for smaller image
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine

# Install curl for health checks and timezone data
RUN apk add --no-cache curl tzdata

# Create app user for security
RUN addgroup -g 1001 app && \
    adduser -D -u 1001 -G app app

# Set working directory
WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R app:app /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy configuration files
COPY --from=build /app/src/main/resources/application*.yml ./

# Change ownership to app user
RUN chown -R app:app /app

# Switch to app user
USER app

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM optimization for containerized environment (Java 17 compatible)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]