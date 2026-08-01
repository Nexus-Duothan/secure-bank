#!/usr/bin/env bash

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=====================================================${NC}"
echo -e "${RED}      SecureBank Platform - Stopping System         ${NC}"
echo -e "${BLUE}=====================================================${NC}"

if [ -d "logs" ]; then
  for pid_file in logs/*.pid; do
    if [ -f "$pid_file" ]; then
      PID=$(cat "$pid_file")
      NAME=$(basename "$pid_file" .pid)
      if kill -0 "$PID" 2>/dev/null; then
        echo -e "Stopping ${RED}${NAME}${NC} (PID: ${PID})..."
        kill "$PID" 2>/dev/null || kill -9 "$PID" 2>/dev/null
      fi
      rm -f "$pid_file"
    fi
  done
fi

echo -e "\nStopping Docker Infrastructure..."
docker compose down

echo -e "${GREEN}✔ All services stopped successfully.${NC}"
