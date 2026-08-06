FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY backend/accounts-service/pom.xml backend/accounts-service/
RUN mvn dependency:go-offline -f backend/accounts-service/pom.xml || true

COPY backend/accounts-service/src backend/accounts-service/src
RUN mvn package -f backend/accounts-service/pom.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/accounts-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
