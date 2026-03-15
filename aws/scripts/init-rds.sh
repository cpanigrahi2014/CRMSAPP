#!/usr/bin/env bash
###############################################################################
# init-rds.sh — Create all CRM databases on the RDS instance
#
# Prerequisites: psql client installed, RDS endpoint reachable
# Usage: ./init-rds.sh <rds-endpoint> <db-password>
###############################################################################
set -euo pipefail

RDS_ENDPOINT="${1:?Usage: $0 <rds-endpoint> <db-password>}"
DB_PASSWORD="${2:?Usage: $0 <rds-endpoint> <db-password>}"
DB_USER="${DB_USER:-postgres}"
DB_PORT="${DB_PORT:-5432}"

DATABASES=(
  crm_auth
  crm_leads
  crm_accounts
  crm_contacts
  crm_opportunities
  crm_activities
  crm_notifications
  crm_workflows
  crm_ai
  crm_email
  crm_integrations
  crm_ai_agent
  crm_cases
  crm_campaigns
)

echo "============================================"
echo "  Initializing RDS databases"
echo "  Endpoint: ${RDS_ENDPOINT}"
echo "============================================"

export PGPASSWORD="$DB_PASSWORD"

for db in "${DATABASES[@]}"; do
  echo "→ Creating database: ${db}"
  psql -h "$RDS_ENDPOINT" -p "$DB_PORT" -U "$DB_USER" -d postgres \
    -tc "SELECT 1 FROM pg_database WHERE datname = '${db}'" | \
    grep -q 1 || \
    psql -h "$RDS_ENDPOINT" -p "$DB_PORT" -U "$DB_USER" -d postgres \
      -c "CREATE DATABASE ${db};"
  echo "  ✓ ${db}"
done

unset PGPASSWORD

echo ""
echo "============================================"
echo "  All ${#DATABASES[@]} databases created"
echo "============================================"
