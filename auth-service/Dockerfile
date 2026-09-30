# ==========================================
# Stage 1: Build auth-service & dependencies
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM files for dependency resolution caching
COPY mvnw ./
COPY mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
COPY auth-service auth-service

# Make mvnw executable and build the application
RUN chmod +x ./mvnw && \
    ./mvnw clean package -DskipTests -pl auth-service -am

# ==========================================
# Stage 2: Minimal Runtime Container
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/auth-service/target/*.jar app.jar

EXPOSE 8081

ENV SERVER_PORT=8081 \
    PORT=8081 \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
