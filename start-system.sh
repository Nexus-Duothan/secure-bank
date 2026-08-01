#!/usr/bin/env bash
set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=====================================================${NC}"
echo -e "${GREEN}      SecureBank Platform - Master Launcher          ${NC}"
echo -e "${BLUE}=====================================================${NC}"

mkdir -p logs

echo -e "\n${YELLOW}[1/4] Starting Docker Infrastructure (PostgreSQL, Kafka, Elasticsearch)...${NC}"
docker compose up -d

echo -e "\n${YELLOW}[2/4] Building Backend Microservices...${NC}"
mvn clean package -DskipTests -q

echo -e "\n${YELLOW}[3/4] Launching Backend Services...${NC}"

start_service() {
  local NAME=$1
  local PORT=$2
  local PATH_TO_JAR=$3
  echo -e "  -> Starting ${GREEN}${NAME}${NC} on port ${BLUE}${PORT}${NC}..."
  nohup java -jar "${PATH_TO_JAR}" > "logs/${NAME}.log" 2>&1 &
  echo $! > "logs/${NAME}.pid"
}

start_service "api-gateway" "8080" "backend/api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar"
start_service "auth-service" "8081" "backend/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar"
start_service "totp-service" "8082" "backend/totp-service/target/totp-service-1.0.0-SNAPSHOT.jar"
start_service "user-service" "8083" "backend/user-service/target/user-service-1.0.0-SNAPSHOT.jar"
start_service "accounts-service" "8084" "backend/accounts-service/target/accounts-service-1.0.0-SNAPSHOT.jar"
start_service "transfer-service" "8085" "backend/transfer-service/target/transfer-service-1.0.0-SNAPSHOT.jar"
start_service "payments-service" "8086" "backend/payments-service/target/payments-service-1.0.0-SNAPSHOT.jar"
start_service "lending-service" "8087" "backend/lending-service/target/lending-service-1.0.0-SNAPSHOT.jar"
start_service "notification-service" "8088" "backend/notification-service/target/notification-service-1.0.0-SNAPSHOT.jar"

echo -e "\n${YELLOW}[4/4] Starting Frontend Web Service...${NC}"
(cd frontend/web-service && npm run dev > ../../logs/web-service.log 2>&1 & echo $! > ../../logs/web-service.pid)

echo -e "\n${BLUE}=====================================================${NC}"
echo -e "${GREEN}   ✔ All Services Launched Successfully!             ${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo -e "🌐 Frontend SPA:    ${GREEN}http://localhost:5173${NC}"
echo -e "🚪 API Gateway:     ${GREEN}http://localhost:8080${NC}"
echo -e "📊 Kibana Audit:    ${GREEN}http://localhost:5601${NC}"
echo -e "🐘 PostgreSQL:      ${GREEN}localhost:5432${NC}"
echo -e "📨 Kafka Broker:    ${GREEN}localhost:9092${NC}"
echo -e "\n📁 Logs are written to: ${YELLOW}logs/*.log${NC}"
echo -e "🛑 To stop all services run: ${RED}npm run stop:all${NC} or ${RED}./stop-system.sh${NC}"
