#!/usr/bin/env bash
set -e

echo "============================================================"
echo "🚀 SecureBank Platform: Local Kubernetes Deployment Script"
echo "============================================================"

# Check for Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Error: Docker is not installed or not in PATH."
    exit 1
fi

# Check for kubectl
if ! command -v kubectl &> /dev/null; then
    echo "❌ Error: kubectl is not installed or not in PATH."
    exit 1
fi

echo "📦 Step 1: Building Docker images..."

echo " -> Building api-gateway..."
docker build -t securebank/api-gateway:latest -f backend/api-gateway/Dockerfile .

echo " -> Building auth-service..."
docker build -t securebank/auth-service:latest -f backend/auth-service/Dockerfile .

echo " -> Building totp-service..."
docker build -t securebank/totp-service:latest -f backend/totp-service/Dockerfile .

echo " -> Building user-service..."
docker build -t securebank/user-service:latest -f backend/user-service/Dockerfile .

echo " -> Building accounts-service..."
docker build -t securebank/accounts-service:latest -f backend/accounts-service/Dockerfile .

echo " -> Building transfer-service..."
docker build -t securebank/transfer-service:latest -f backend/transfer-service/Dockerfile .

echo " -> Building payments-service..."
docker build -t securebank/payments-service:latest -f backend/payments-service/Dockerfile .

echo " -> Building lending-service..."
docker build -t securebank/lending-service:latest -f backend/lending-service/Dockerfile .

echo " -> Building notification-service..."
docker build -t securebank/notification-service:latest -f backend/notification-service/Dockerfile .

echo " -> Building audit-recovery-service..."
docker build -t securebank/audit-recovery-service:latest -f security/audit-recovery-service/Dockerfile .

echo " -> Building frontend-web..."
docker build -t securebank/frontend-web:latest -f frontend/web-service/Dockerfile .

echo "☸️ Step 2: Applying Kubernetes Manifests..."
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmaps-secrets.yaml
kubectl apply -f k8s/02-infrastructure.yaml
kubectl apply -f k8s/03-backend-services.yaml
kubectl apply -f k8s/04-frontend.yaml
kubectl apply -f k8s/05-ingress.yaml

echo "⏳ Step 3: Checking pod status..."
kubectl get pods -n securebank

echo "============================================================"
echo "✅ Local Kubernetes deployment manifests applied!"
echo "Run: kubectl get pods -n securebank -w to monitor rollout."
echo "============================================================"
