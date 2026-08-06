FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY backend/api-gateway/pom.xml backend/api-gateway/
RUN mvn dependency:go-offline -f backend/api-gateway/pom.xml || true

COPY backend/api-gateway/src backend/api-gateway/src
RUN mvn package -f backend/api-gateway/pom.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
