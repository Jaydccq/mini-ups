# Multi-stage build for Spring Boot application
FROM maven:3.9.6-eclipse-temurin-17 AS build

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
RUN mvn clean package -DskipTests=true -Dmaven.test.skip=true -Dmaven.gitcommitid.skip=true

# Runtime stage with JRE for smaller image
FROM eclipse-temurin:17-jre

# Install curl for health checks and timezone data
RUN apt-get update && apt-get install -y curl tzdata && rm -rf /var/lib/apt/lists/*

# Create app user for security
RUN groupadd -g 1001 app && \
    useradd -r -u 1001 -g app app

# Set working directory
WORKDIR /app

# Create logs directory and Sentinel CSP directory
RUN mkdir -p /app/logs /app/logs/csp /home/app/logs /home/app/logs/csp && \
    chown -R app:app /app /home/app

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
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
