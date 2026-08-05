FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY backend/lending-service/pom.xml backend/lending-service/
RUN mvn dependency:go-offline -f backend/lending-service/pom.xml || true

COPY backend/lending-service/src backend/lending-service/src
RUN mvn package -f backend/lending-service/pom.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/backend/lending-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
