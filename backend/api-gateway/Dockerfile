# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY backend/api-gateway/pom.xml backend/api-gateway/
COPY backend/auth-service/pom.xml backend/auth-service/
COPY backend/totp-service/pom.xml backend/totp-service/
COPY backend/user-service/pom.xml backend/user-service/
COPY backend/accounts-service/pom.xml backend/accounts-service/
COPY backend/transfer-service/pom.xml backend/transfer-service/
COPY backend/payments-service/pom.xml backend/payments-service/
COPY backend/lending-service/pom.xml backend/lending-service/
COPY backend/notification-service/pom.xml backend/notification-service/

COPY backend/api-gateway/src backend/api-gateway/src

RUN ./mvnw package -pl backend/api-gateway -DskipTests || apk add --no-cache maven && mvn package -pl backend/api-gateway -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/backend/api-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
