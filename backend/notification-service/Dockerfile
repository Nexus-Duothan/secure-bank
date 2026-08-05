FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY backend/notification-service/pom.xml backend/notification-service/
RUN mvn dependency:go-offline -f backend/notification-service/pom.xml || true

COPY backend/notification-service/src backend/notification-service/src
RUN mvn package -f backend/notification-service/pom.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/notification-service/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
