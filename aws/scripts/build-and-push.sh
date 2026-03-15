#!/usr/bin/env bash
###############################################################################
# build-and-push.sh — Build Docker images and push to ECR
#
# Usage:
#   ./build-and-push.sh                    # Build & push all services
#   ./build-and-push.sh auth-service       # Build & push one service
#   ./build-and-push.sh --tag v1.2.3       # Custom tag
###############################################################################
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
ECR_REGISTRY="${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com"
PROJECT_NAME="crm"
IMAGE_TAG="latest"

# Service → build context mapping
declare -A SERVICE_CONTEXTS=(
  ["auth-service"]="auth-service"
  ["lead-service"]="lead-service"
  ["account-service"]="account-service"
  ["contact-service"]="contact-service"
  ["opportunity-service"]="opportunity-service"
  ["activity-service"]="activity-service"
  ["notification-service"]="notification-service"
  ["workflow-service"]="workflow-service"
  ["ai-service"]="ai-service"
  ["email-service"]="email-service"
  ["integration-service"]="integration-service"
  ["case-service"]="case-service"
  ["campaign-service"]="campaign-service"
  ["ai-agent"]="crm-ai-agent"
  ["frontend"]="crm-frontend"
)

# Parse args
TARGET_SERVICE=""
while [[ $# -gt 0 ]]; do
  case $1 in
    --tag) IMAGE_TAG="$2"; shift 2 ;;
    --region) AWS_REGION="$2"; shift 2 ;;
    *) TARGET_SERVICE="$1"; shift ;;
  esac
done

echo "============================================"
echo "  CRM Platform — Build & Push to ECR"
echo "============================================"
echo "  Registry : ${ECR_REGISTRY}"
echo "  Tag      : ${IMAGE_TAG}"
echo "  Region   : ${AWS_REGION}"
echo "============================================"

# Login to ECR
echo "→ Logging in to ECR..."
aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$ECR_REGISTRY"

build_and_push() {
  local svc="$1"
  local context="${SERVICE_CONTEXTS[$svc]}"
  local repo="${PROJECT_NAME}/${svc}"
  local full_image="${ECR_REGISTRY}/${repo}:${IMAGE_TAG}"

  echo ""
  echo "─── Building ${svc} ───"
  cd "$PROJECT_ROOT"

  docker build -t "${repo}:${IMAGE_TAG}" "./${context}"
  docker tag "${repo}:${IMAGE_TAG}" "$full_image"

  echo "→ Pushing ${full_image}..."
  docker push "$full_image"

  # Also tag as 'latest' if using a version tag
  if [[ "$IMAGE_TAG" != "latest" ]]; then
    docker tag "${repo}:${IMAGE_TAG}" "${ECR_REGISTRY}/${repo}:latest"
    docker push "${ECR_REGISTRY}/${repo}:latest"
  fi

  echo "✓ ${svc} pushed successfully"
}

if [[ -n "$TARGET_SERVICE" ]]; then
  if [[ -z "${SERVICE_CONTEXTS[$TARGET_SERVICE]+x}" ]]; then
    echo "ERROR: Unknown service '${TARGET_SERVICE}'"
    echo "Valid services: ${!SERVICE_CONTEXTS[*]}"
    exit 1
  fi
  build_and_push "$TARGET_SERVICE"
else
  for svc in "${!SERVICE_CONTEXTS[@]}"; do
    build_and_push "$svc"
  done
fi

echo ""
echo "============================================"
echo "  All images pushed successfully!"
echo "============================================"
