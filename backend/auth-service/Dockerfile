FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY backend/auth-service/pom.xml backend/auth-service/
RUN mvn dependency:go-offline -f backend/auth-service/pom.xml || true

COPY backend/auth-service/src backend/auth-service/src
RUN mvn package -f backend/auth-service/pom.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
