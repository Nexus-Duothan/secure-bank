#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Local Kubernetes deployment.
#
# Images are pushed through a throwaway registry on localhost:5000 rather than
# being left in the local Docker image store. Docker Desktop now keeps built
# images in containerd's own namespace, which kubelet does not read, so a
# locally built image is invisible to the cluster and it silently keeps running
# whatever stale copy it already had. Pushing to a registry the node can pull
# from is the only reliable way to get today's code into the cluster.
#
# Every build gets a unique tag, so each deploy is a distinct image reference
# and Kubernetes always rolls the pods rather than reusing a cached layer.
#
# The manifests are left untouched (they carry the registry-less names used by
# the GCP deployment); the image is repointed afterwards with `kubectl set
# image`.
# ============================================================================

NAMESPACE="${NAMESPACE:-securebank}"
REGISTRY="${REGISTRY:-localhost:5000}"
REGISTRY_CONTAINER="${REGISTRY_CONTAINER:-securebank-registry}"
IMAGE_TAG="${IMAGE_TAG:-$(date +%Y%m%d%H%M%S)}"

# service:dockerfile — the deployment and container names match the service name.
SERVICES=(
  "api-gateway:backend/api-gateway/Dockerfile"
  "auth-service:backend/auth-service/Dockerfile"
  "totp-service:backend/totp-service/Dockerfile"
  "user-service:backend/user-service/Dockerfile"
  "accounts-service:backend/accounts-service/Dockerfile"
  "transfer-service:backend/transfer-service/Dockerfile"
  "payments-service:backend/payments-service/Dockerfile"
  "lending-service:backend/lending-service/Dockerfile"
  "notification-service:backend/notification-service/Dockerfile"
  "audit-recovery-service:security/audit-recovery-service/Dockerfile"
  "frontend-web:frontend/web-service/Dockerfile"
)

echo "============================================================"
echo "SecureBank Platform: Local Kubernetes Deployment"
echo "============================================================"
echo "Namespace: ${NAMESPACE}"
echo "Registry:  ${REGISTRY}"
echo "Tag:       ${IMAGE_TAG}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: Docker is not installed or not in PATH." >&2
  exit 1
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "Error: kubectl is not installed or not in PATH." >&2
  exit 1
fi

# --------------------------------------------------------------------------
# Step 1: the registry the cluster pulls from
# --------------------------------------------------------------------------
if [[ "${REGISTRY}" == "localhost:5000" ]]; then
  echo ""
  echo "Step 1: Ensuring the local registry is running..."
  if [[ -z "$(docker ps -q -f "name=^${REGISTRY_CONTAINER}$")" ]]; then
    docker rm -f "${REGISTRY_CONTAINER}" >/dev/null 2>&1 || true
    docker run -d -p 5000:5000 --restart=always --name "${REGISTRY_CONTAINER}" registry:2 >/dev/null
    echo " -> Started ${REGISTRY_CONTAINER}"
  else
    echo " -> Already running"
  fi

  for _ in $(seq 1 30); do
    if curl -fs -o /dev/null "http://${REGISTRY}/v2/"; then break; fi
    sleep 1
  done
  if ! curl -fs -o /dev/null "http://${REGISTRY}/v2/"; then
    echo "Error: the registry at ${REGISTRY} did not come up." >&2
    exit 1
  fi
else
  echo ""
  echo "Step 1: Using the supplied registry (skipping the local one)."
fi

# --------------------------------------------------------------------------
# Step 2: build and push
# --------------------------------------------------------------------------
echo ""
echo "Step 2: Building and pushing images..."
for entry in "${SERVICES[@]}"; do
  service="${entry%%:*}"
  dockerfile="${entry#*:}"
  image="${REGISTRY}/securebank/${service}:${IMAGE_TAG}"

  echo " -> Building ${service}..."
  docker build -q -t "${image}" -t "securebank/${service}:latest" -f "${dockerfile}" . >/dev/null

  echo " -> Pushing ${service}..."
  docker push -q "${image}" >/dev/null
done

# --------------------------------------------------------------------------
# Step 3: manifests
# --------------------------------------------------------------------------
echo ""
echo "Step 3: Applying Kubernetes manifests..."
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmaps-secrets.yaml
kubectl apply -f k8s/02-infrastructure.yaml
kubectl apply -f k8s/03-backend-services.yaml
kubectl apply -f k8s/04-frontend.yaml
kubectl apply -f k8s/05-ingress.yaml

# --------------------------------------------------------------------------
# Step 4: point the deployments at the images just pushed
# --------------------------------------------------------------------------
echo ""
echo "Step 4: Pointing deployments at ${REGISTRY} (tag ${IMAGE_TAG})..."
for entry in "${SERVICES[@]}"; do
  service="${entry%%:*}"
  kubectl -n "${NAMESPACE}" set image "deploy/${service}" \
    "${service}=${REGISTRY}/securebank/${service}:${IMAGE_TAG}" >/dev/null
done

# --------------------------------------------------------------------------
# Step 5: wait for the new pods
# --------------------------------------------------------------------------
echo ""
echo "Step 5: Waiting for rollouts..."
echo " -> Infrastructure first, so the services find a database to migrate."
kubectl -n "${NAMESPACE}" rollout status deploy/securebank-postgres --timeout=300s

for entry in "${SERVICES[@]}"; do
  service="${entry%%:*}"
  if ! kubectl -n "${NAMESPACE}" rollout status "deploy/${service}" --timeout=300s; then
    echo ""
    echo "Error: ${service} did not become ready. Recent logs:" >&2
    kubectl -n "${NAMESPACE}" logs "deploy/${service}" --tail=40 >&2 || true
    exit 1
  fi
done

echo ""
kubectl get pods -n "${NAMESPACE}"

echo "============================================================"
echo "Deployment complete (tag ${IMAGE_TAG})."
echo ""
echo "Seed demo data:"
echo "  ./scripts/seed-k8s-local.sh"
echo ""
# The frontend Service is a NodePort, but Docker Desktop does not publish node ports to the
# Windows host, and no ingress controller is installed, so the Ingress has no address either.
# A port-forward is the one route in that works on a stock Docker Desktop cluster.
echo "Open the app (leave this running, then browse to http://localhost:8080):"
echo "  kubectl -n ${NAMESPACE} port-forward svc/frontend-web 8080:80"
echo "============================================================"
