FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY backend/payments-service/pom.xml backend/payments-service/
RUN mvn dependency:go-offline -f backend/payments-service/pom.xml || true

COPY backend/payments-service/src backend/payments-service/src
RUN mvn package -f backend/payments-service/pom.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/payments-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
