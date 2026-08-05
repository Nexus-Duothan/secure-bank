FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY backend/payments-service/pom.xml backend/payments-service/
COPY backend/payments-service/src backend/payments-service/src
RUN apk add --no-cache maven && mvn package -pl backend/payments-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/payments-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
