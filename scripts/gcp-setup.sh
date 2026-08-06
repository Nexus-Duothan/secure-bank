#!/usr/bin/env bash
set -eo pipefail

# ==============================================================================
# SecureBank GCP Infrastructure Setup & Workload Identity Federation Script
# ==============================================================================

# Default configurations
GCP_REGION="${GCP_REGION:-us-central1}"
GCP_ZONE="${GCP_ZONE:-us-central1-a}"
CLUSTER_NAME="${CLUSTER_NAME:-securebank-cluster}"
REPOSITORY_NAME="${REPOSITORY_NAME:-secure-bank-repo}"
SA_NAME="github-actions-deployer"
POOL_NAME="github-pool"
PROVIDER_NAME="github-provider"

echo "======================================================================"
echo "🏦 SecureBank GCP Environment Setup"
echo "======================================================================"

# Check gcloud CLI installation
if ! command -v gcloud &> /dev/null; then
    echo "❌ Error: gcloud CLI is not installed. Please install it first."
    exit 1
fi

# Get current project
PROJECT_ID=$(gcloud config get-value project 2>/dev/null || true)
if [ -z "$PROJECT_ID" ]; then
    read -p "Enter your GCP Project ID: " PROJECT_ID
    gcloud config set project "$PROJECT_ID"
fi

echo "🔹 Operating on GCP Project: $PROJECT_ID"
echo "🔹 Target Region: $GCP_REGION ($GCP_ZONE)"

# 1. Enable Required GCP APIs
echo ""
echo "1️⃣  Enabling GCP Service APIs..."
gcloud services enable \
    container.googleapis.com \
    artifactregistry.googleapis.com \
    cloudbuild.googleapis.com \
    iamcredentials.googleapis.com \
    sqladmin.googleapis.com \
    compute.googleapis.com

# 2. Create Artifact Registry Repository
echo ""
echo "2️⃣  Creating Artifact Registry repository..."
if ! gcloud artifacts repositories describe "$REPOSITORY_NAME" --location="$GCP_REGION" &>/dev/null; then
    gcloud artifacts repositories create "$REPOSITORY_NAME" \
        --repository-format=docker \
        --location="$GCP_REGION" \
        --description="Docker repository for SecureBank microservices"
    echo "✅ Created Artifact Registry: $REPOSITORY_NAME"
else
    echo "ℹ️  Artifact Registry '$REPOSITORY_NAME' already exists."
fi

# 3. Create GKE Cluster
echo ""
echo "3️⃣  Provisioning GKE Cluster ($CLUSTER_NAME)..."
if ! gcloud container clusters describe "$CLUSTER_NAME" --zone="$GCP_ZONE" &>/dev/null; then
    gcloud container clusters create "$CLUSTER_NAME" \
        --zone="$GCP_ZONE" \
        --num-nodes=2 \
        --machine-type="e2-standard-4" \
        --disk-size=50GB \
        --enable-autoscaling --min-nodes=1 --max-nodes=3 \
        --scopes="https://www.googleapis.com/auth/cloud-platform" \
        --labels="env=production,app=securebank"
    echo "✅ GKE Cluster '$CLUSTER_NAME' created successfully."
else
    echo "ℹ️  GKE Cluster '$CLUSTER_NAME' already exists."
fi

# 4. Create Service Account for GitHub Actions
echo ""
echo "4️⃣  Configuring Service Account for GitHub Actions..."
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
if ! gcloud iam service-accounts describe "$SA_EMAIL" &>/dev/null; then
    gcloud iam service-accounts create "$SA_NAME" \
        --display-name="GitHub Actions Deployer SA"
    echo "✅ Service account created: $SA_EMAIL"
else
    echo "ℹ️  Service account '$SA_EMAIL' already exists."
fi

# Grant IAM Roles to Service Account
echo "   Assigning required IAM roles..."
ROLES=(
    "roles/artifactregistry.writer"
    "roles/container.developer"
    "roles/cloudbuild.builds.editor"
    "roles/storage.admin"
)

for ROLE in "${ROLES[@]}"; do
    gcloud projects add-iam-policy-binding "$PROJECT_ID" \
        --member="serviceAccount:${SA_EMAIL}" \
        --role="$ROLE" \
        --condition=None &>/dev/null
done

# Also grant Cloud Build SA permissions to write to Artifact Registry
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")
BUILD_SA="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${BUILD_SA}" \
    --role="roles/artifactregistry.writer" \
    --condition=None &>/dev/null || true

# 5. Create Workload Identity Pool & Provider
echo ""
echo "5️⃣  Setting up Workload Identity Federation..."
if ! gcloud iam workload-identity-pools describe "$POOL_NAME" --location="global" &>/dev/null; then
    gcloud iam workload-identity-pools create "$POOL_NAME" \
        --location="global" \
        --display-name="GitHub Actions Pool"
fi

WORKLOAD_IDENTITY_POOL_ID=$(gcloud iam workload-identity-pools describe "$POOL_NAME" \
    --location="global" \
    --format="value(name)")

if ! gcloud iam workload-identity-pools providers describe "$PROVIDER_NAME" \
    --location="global" \
    --workload-identity-pool="$POOL_NAME" &>/dev/null; then
    gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_NAME" \
        --location="global" \
        --workload-identity-pool="$POOL_NAME" \
        --display-name="GitHub Provider" \
        --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository" \
        --attribute-condition="assertion.repository != ''" \
        --issuer-uri="https://token.actions.githubusercontent.com"
fi

WORKLOAD_IDENTITY_PROVIDER=$(gcloud iam workload-identity-pools providers describe "$PROVIDER_NAME" \
    --location="global" \
    --workload-identity-pool="$POOL_NAME" \
    --format="value(name)")

# Prompt for GitHub Repository (Owner/Repo format)
read -p "Enter your GitHub Repository (e.g. Nexus-Duothan/secure-bank): " GITHUB_REPO

# Allow GitHub Actions repo to impersonate Service Account
gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
    --role="roles/iam.workloadIdentityUser" \
    --member="principalSet://iam.googleapis.com/${WORKLOAD_IDENTITY_POOL_ID}/attribute.repository/${GITHUB_REPO}" \
    --condition=None &>/dev/null

echo ""
echo "======================================================================"
echo "🎉 GCP Setup Completed Successfully!"
echo "======================================================================"
echo "Please add the following SECRETS to your GitHub Repository ($GITHUB_REPO):"
echo ""
echo "  1️⃣  GCP_PROJECT_ID:"
echo "      $PROJECT_ID"
echo ""
echo "  2️⃣  GCP_WORKLOAD_IDENTITY_PROVIDER:"
echo "      $WORKLOAD_IDENTITY_PROVIDER"
echo ""
echo "  3️⃣  GCP_SERVICE_ACCOUNT:"
echo "      $SA_EMAIL"
echo ""
echo "======================================================================"
