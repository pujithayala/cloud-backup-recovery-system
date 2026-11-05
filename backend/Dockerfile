# Multi-stage Dockerfile for Cloud Backup Backend
# Stage 1: Build the Spring Boot application
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Cache maven dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production ja
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal hardened JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for local blob storage and set permissions
RUN mkdir -p /app/data/cloud-storage && chown -R appuser:appgroup /app

USER appuser

COPY --from=builder /workspace/target/cloud-backup-backend-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 0

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

