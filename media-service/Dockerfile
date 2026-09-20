# ==========================================
# Stage 1: Build media-service & dependencies
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM files for dependency resolution caching
COPY mvnw ./
COPY mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
COPY common-lib common-lib
COPY media-service media-service

# Make mvnw executable and build the application
RUN chmod +x ./mvnw && \
    ./mvnw clean package -DskipTests -pl common-lib,media-service -am

# ==========================================
# Stage 2: Minimal Runtime Container
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built artifact from builder stage
COPY --from=builder /workspace/media-service/target/*.jar app.jar

# Default environment configuration
ENV SERVER_PORT=8084 \
    PORT=8084 \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8084

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
