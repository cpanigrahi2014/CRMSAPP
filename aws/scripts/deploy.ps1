###############################################################################
# deploy.ps1 — Windows PowerShell deployment script
#
# Usage:
#   .\deploy.ps1                           # Full deploy
#   .\deploy.ps1 -SkipInfra                # Skip terraform
#   .\deploy.ps1 -Service auth-service     # Update single service
#   .\deploy.ps1 -SkipBuild                # Only update ECS
###############################################################################
param(
    [switch]$SkipInfra,
    [switch]$SkipBuild,
    [string]$Service = "",
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TerraformDir = Join-Path $ScriptDir "..\terraform"
$ProjectRoot = Join-Path $ScriptDir "..\.."

$ServiceContexts = @{
    "auth-service"         = "auth-service"
    "lead-service"         = "lead-service"
    "account-service"      = "account-service"
    "contact-service"      = "contact-service"
    "opportunity-service"  = "opportunity-service"
    "activity-service"     = "activity-service"
    "notification-service" = "notification-service"
    "workflow-service"     = "workflow-service"
    "ai-service"           = "ai-service"
    "email-service"        = "email-service"
    "integration-service"  = "integration-service"
    "case-service"         = "case-service"
    "campaign-service"     = "campaign-service"
    "ai-agent"             = "crm-ai-agent"
    "frontend"             = "crm-frontend"
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  CRM Platform - AWS Deployment" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# ── Step 1: Terraform ────────────────────────────────────────────────────────

if (-not $SkipInfra) {
    Write-Host "=== Step 1: Provisioning Infrastructure ===" -ForegroundColor Yellow
    Push-Location $TerraformDir

    if (-not (Test-Path "terraform.tfvars")) {
        Write-Host "ERROR: terraform.tfvars not found." -ForegroundColor Red
        Write-Host "Copy terraform.tfvars.example to terraform.tfvars and fill in values."
        Pop-Location
        exit 1
    }

    terraform init -upgrade
    terraform plan -out=tfplan
    $confirm = Read-Host "Apply this plan? (yes/no)"
    if ($confirm -ne "yes") {
        Write-Host "Aborted." -ForegroundColor Yellow
        Remove-Item -Force tfplan -ErrorAction SilentlyContinue
        Pop-Location
        exit 0
    }
    terraform apply tfplan
    Remove-Item -Force tfplan -ErrorAction SilentlyContinue
    Pop-Location
}

Push-Location $TerraformDir
$RdsEndpoint = terraform output -raw rds_endpoint
$EcsCluster  = terraform output -raw ecs_cluster_name
$AppUrl      = terraform output -raw app_url
Pop-Location

Write-Host "  RDS Endpoint : $RdsEndpoint"
Write-Host "  ECS Cluster  : $EcsCluster"

# ── Step 2: Build & Push ─────────────────────────────────────────────────────

if (-not $SkipBuild) {
    Write-Host "`n=== Step 2: Building & Pushing Docker Images ===" -ForegroundColor Yellow

    $AwsAccount = aws sts get-caller-identity --query Account --output text
    $EcrRegistry = "$AwsAccount.dkr.ecr.$Region.amazonaws.com"

    Write-Host "Logging in to ECR..."
    aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $EcrRegistry

    $servicesToBuild = if ($Service) { @{ $Service = $ServiceContexts[$Service] } } else { $ServiceContexts }

    foreach ($svc in $servicesToBuild.Keys) {
        $context = $servicesToBuild[$svc]
        $repo = "crm/$svc"
        $fullImage = "$EcrRegistry/${repo}:latest"

        Write-Host "`n--- Building $svc ---" -ForegroundColor Green
        Push-Location $ProjectRoot
        docker build -t "${repo}:latest" "./$context"
        docker tag "${repo}:latest" $fullImage
        docker push $fullImage
        Pop-Location
        Write-Host "  Done: $svc" -ForegroundColor Green
    }
}

# ── Step 3: Deploy to ECS ────────────────────────────────────────────────────

Write-Host "`n=== Step 3: Deploying to ECS ===" -ForegroundColor Yellow

$allServices = @(
    "auth-service", "lead-service", "account-service", "contact-service",
    "opportunity-service", "activity-service", "notification-service",
    "workflow-service", "ai-service", "email-service", "integration-service",
    "case-service", "campaign-service", "ai-agent", "frontend"
)

$deployServices = if ($Service) { @($Service) } else { $allServices }

foreach ($svc in $deployServices) {
    Write-Host "  Updating: $svc"
    aws ecs update-service `
        --cluster $EcsCluster `
        --service $svc `
        --force-new-deployment `
        --region $Region `
        --no-cli-pager | Out-Null
    Write-Host "  Done: $svc" -ForegroundColor Green
}

Write-Host "`nWaiting for services to stabilize..." -ForegroundColor Yellow
foreach ($svc in $deployServices) {
    Write-Host "  Waiting: $svc"
    aws ecs wait services-stable --cluster $EcsCluster --services $svc --region $Region 2>$null
}

# ── Done ─────────────────────────────────────────────────────────────────────

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Deployment Complete!" -ForegroundColor Cyan
Write-Host "  URL: $AppUrl" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan
