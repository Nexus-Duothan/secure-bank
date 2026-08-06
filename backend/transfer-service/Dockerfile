FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY backend/transfer-service/pom.xml backend/transfer-service/
RUN mvn dependency:go-offline -f backend/transfer-service/pom.xml || true

COPY backend/transfer-service/src backend/transfer-service/src
RUN mvn package -f backend/transfer-service/pom.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/transfer-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
