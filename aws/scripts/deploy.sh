#!/usr/bin/env bash
###############################################################################
# deploy.sh — Full deployment pipeline
#
# Steps:
#   1. terraform apply (infra)
#   2. init RDS databases
#   3. build & push Docker images
#   4. force ECS service update (new deployment)
#
# Usage:
#   ./deploy.sh                    # Full deploy
#   ./deploy.sh --skip-infra       # Skip terraform, only update services
#   ./deploy.sh --service auth-service  # Update single service
###############################################################################
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform"
AWS_REGION="${AWS_REGION:-us-east-1}"

SKIP_INFRA=false
SKIP_BUILD=false
TARGET_SERVICE=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-infra) SKIP_INFRA=true; shift ;;
    --skip-build) SKIP_BUILD=true; shift ;;
    --service) TARGET_SERVICE="$2"; shift 2 ;;
    --region) AWS_REGION="$2"; shift 2 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

echo "╔══════════════════════════════════════════╗"
echo "║   CRM Platform — AWS Deployment          ║"
echo "╚══════════════════════════════════════════╝"

# ── Step 1: Terraform ────────────────────────────────────────────────────────

if [[ "$SKIP_INFRA" == false ]]; then
  echo ""
  echo "═══ Step 1: Provisioning Infrastructure ═══"
  cd "$TERRAFORM_DIR"

  if [[ ! -f terraform.tfvars ]]; then
    echo "ERROR: terraform.tfvars not found."
    echo "Copy terraform.tfvars.example to terraform.tfvars and fill in values."
    exit 1
  fi

  terraform init -upgrade
  terraform plan -out=tfplan
  echo ""
  read -rp "Apply this plan? (yes/no): " CONFIRM
  if [[ "$CONFIRM" != "yes" ]]; then
    echo "Aborted."
    exit 0
  fi
  terraform apply tfplan
  rm -f tfplan

  # Capture outputs
  RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
  ECS_CLUSTER=$(terraform output -raw ecs_cluster_name)
else
  echo ""
  echo "═══ Step 1: Skipping infra (--skip-infra) ═══"
  cd "$TERRAFORM_DIR"
  RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
  ECS_CLUSTER=$(terraform output -raw ecs_cluster_name)
fi

echo "  RDS Endpoint  : $RDS_ENDPOINT"
echo "  ECS Cluster   : $ECS_CLUSTER"

# ── Step 2: Initialize databases ─────────────────────────────────────────────

if [[ "$SKIP_INFRA" == false ]]; then
  echo ""
  echo "═══ Step 2: Initializing RDS Databases ═══"
  DB_PASSWORD=$(terraform output -raw 2>/dev/null || true)
  if [[ -z "$DB_PASSWORD" ]]; then
    read -rsp "Enter RDS master password: " DB_PASSWORD
    echo ""
  fi

  # The RDS host is in format "host:port"
  RDS_HOST="${RDS_ENDPOINT%%:*}"
  "$SCRIPT_DIR/init-rds.sh" "$RDS_HOST" "$DB_PASSWORD"
fi

# ── Step 3: Build & Push images ──────────────────────────────────────────────

if [[ "$SKIP_BUILD" == false ]]; then
  echo ""
  echo "═══ Step 3: Building & Pushing Docker Images ═══"
  if [[ -n "$TARGET_SERVICE" ]]; then
    "$SCRIPT_DIR/build-and-push.sh" "$TARGET_SERVICE"
  else
    "$SCRIPT_DIR/build-and-push.sh"
  fi
fi

# ── Step 4: Force new ECS deployment ────────────────────────────────────────

echo ""
echo "═══ Step 4: Deploying to ECS ═══"

ALL_SERVICES=(
  auth-service lead-service account-service contact-service
  opportunity-service activity-service notification-service
  workflow-service ai-service email-service integration-service
  case-service campaign-service ai-agent frontend
)

if [[ -n "$TARGET_SERVICE" ]]; then
  DEPLOY_SERVICES=("$TARGET_SERVICE")
else
  DEPLOY_SERVICES=("${ALL_SERVICES[@]}")
fi

for svc in "${DEPLOY_SERVICES[@]}"; do
  echo "→ Updating ECS service: ${svc}"
  aws ecs update-service \
    --cluster "$ECS_CLUSTER" \
    --service "$svc" \
    --force-new-deployment \
    --region "$AWS_REGION" \
    --no-cli-pager > /dev/null
  echo "  ✓ ${svc}"
done

echo ""
echo "═══ Waiting for services to stabilize... ═══"
echo "(This may take several minutes)"

for svc in "${DEPLOY_SERVICES[@]}"; do
  echo "→ Waiting on: ${svc}"
  aws ecs wait services-stable \
    --cluster "$ECS_CLUSTER" \
    --services "$svc" \
    --region "$AWS_REGION" 2>/dev/null || true
done

# ── Done ─────────────────────────────────────────────────────────────────────

cd "$TERRAFORM_DIR"
APP_URL=$(terraform output -raw app_url 2>/dev/null || echo "unknown")

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   Deployment Complete!                    ║"
echo "╠══════════════════════════════════════════╣"
echo "║   URL: ${APP_URL}"
echo "╚══════════════════════════════════════════╝"
