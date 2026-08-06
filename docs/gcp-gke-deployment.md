# Google Cloud Platform (GCP) GKE Deployment Guide

This guide walks you through deploying the SecureBank platform to **Google Kubernetes Engine (GKE)**.

---

## 📋 Prerequisites

1. **GCP Account & Project** with billing enabled.
2. **`gcloud` CLI** installed and authenticated:
   ```bash
   gcloud auth login
   gcloud config set project YOUR_GCP_PROJECT_ID
   ```
3. **`kubectl`** installed.

---

## 🚀 Step 1: Create GKE Autopilot / Standard Cluster

```bash
# Enable required GCP APIs
gcloud services enable container.googleapis.com artifactregistry.googleapis.com

# Create GKE Autopilot cluster (1 free cluster per billing account)
gcloud container clusters create-auto securebank-cluster \
    --region us-central1

# Get credentials for kubectl
gcloud container clusters get-credentials securebank-cluster --region us-central1
```

---

## 📦 Step 2: Push Container Images to GCP Artifact Registry

```bash
# Create Artifact Registry Repository
gcloud artifacts repositories create securebank-repo \
    --repository-format=docker \
    --location=us-central1 \
    --description="SecureBank Container Repository"

# Configure Docker authentication for GCP Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev

# Tag and push images (Example for api-gateway)
docker tag securebank/api-gateway:latest us-central1-docker.pkg.dev/YOUR_GCP_PROJECT_ID/securebank-repo/api-gateway:latest
docker push us-central1-docker.pkg.dev/YOUR_GCP_PROJECT_ID/securebank-repo/api-gateway:latest
```

---

## ☸️ Step 3: Apply Kubernetes Manifests

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmaps-secrets.yaml
kubectl apply -f k8s/02-infrastructure.yaml
kubectl apply -f k8s/03-backend-services.yaml
kubectl apply -f k8s/04-frontend.yaml
kubectl apply -f k8s/05-ingress.yaml
```

---

## 🔍 Step 4: Verify Deployment

```bash
kubectl get pods -n securebank
kubectl get services -n securebank
kubectl get ingress -n securebank
```
