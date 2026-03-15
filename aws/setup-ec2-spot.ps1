###############################################################################
# setup-ec2-spot.ps1 — Complete EC2 Spot Instance Setup for CRM Testing
#
# This script does everything from your Windows machine:
#   1. Creates a key pair
#   2. Creates a security group
#   3. Launches a t3.xlarge Spot instance
#   4. Attaches an Elastic IP
#   5. Gives you SSH + setup commands
#
# Prerequisites:
#   - AWS CLI v2 installed:  winget install Amazon.AWSCLI
#   - AWS configured:        aws configure
#   - IAM user needs AmazonEC2FullAccess policy attached
#     (go to AWS Console > IAM > Users > your user > Add permissions)
#
# Usage:  .\aws\setup-ec2-spot.ps1
###############################################################################

param(
    [string]$Region       = "us-east-1",
    [string]$InstanceType = "t3.xlarge",
    [string]$KeyName      = "crm-test-key",
    [string]$ProjectName  = "crm-test"
)

$ErrorActionPreference = "Stop"

function Write-Step  { param($msg) Write-Host "`n>>> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "    OK: $msg" -ForegroundColor Green }
function Write-Info  { param($msg) Write-Host "    $msg" -ForegroundColor Yellow }
function Write-Fail  { param($msg) Write-Host "    FAIL: $msg" -ForegroundColor Red }

Write-Host ""
Write-Host "========================================================" -ForegroundColor Magenta
Write-Host "  CRM Platform - EC2 Spot Instance Setup" -ForegroundColor Magenta
Write-Host "  Instance: $InstanceType (4 vCPU, 16 GB RAM)" -ForegroundColor Magenta
Write-Host "  Region:   $Region" -ForegroundColor Magenta
Write-Host "  Cost:     ~`$20-35/month (Spot)" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

###############################################################################
# STEP 0: Verify AWS CLI + Permissions
###############################################################################
Write-Step "Verifying AWS CLI and permissions..."

try {
    $identityJson = aws sts get-caller-identity --output json 2>&1
    if ($LASTEXITCODE -ne 0) { throw "AWS CLI not configured" }
    $identity = $identityJson | ConvertFrom-Json
    Write-Ok "Logged in as: $($identity.Arn)"
} catch {
    Write-Host "`n  ERROR: AWS CLI not configured!" -ForegroundColor Red
    Write-Host "  Run these commands first:" -ForegroundColor Red
    Write-Host "    winget install Amazon.AWSCLI" -ForegroundColor Yellow
    Write-Host "    aws configure" -ForegroundColor Yellow
    Write-Host "    (Enter: Access Key ID, Secret Key, Region: us-east-1, Format: json)" -ForegroundColor Yellow
    exit 1
}

$AccountId = $identity.Account

# Quick permission check
$permCheck = aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --region $Region --query "Vpcs[0].VpcId" --output text 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "  ERROR: Your IAM user lacks EC2 permissions!" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Fix this in the AWS Console:" -ForegroundColor Yellow
    Write-Host "    1. Go to https://console.aws.amazon.com/iam" -ForegroundColor White
    Write-Host "    2. Log in with your ROOT account (the email used to create AWS account)" -ForegroundColor White
    Write-Host "    3. Click 'Users' > '$($identity.Arn.Split('/')[-1])'" -ForegroundColor White
    Write-Host "    4. Click 'Add permissions' > 'Attach policies directly'" -ForegroundColor White
    Write-Host "    5. Search for 'AmazonEC2FullAccess' and check it" -ForegroundColor White
    Write-Host "    6. Click 'Add permissions'" -ForegroundColor White
    Write-Host "    7. Re-run this script" -ForegroundColor White
    Write-Host ""
    exit 1
}
$vpcId = $permCheck
Write-Ok "EC2 permissions verified (VPC: $vpcId)"

###############################################################################
# STEP 1: Create Key Pair
###############################################################################
Write-Step "Creating SSH Key Pair: $KeyName"

$keyFile = "$HOME\.ssh\${KeyName}.pem"

$keyExistsInAws = $false
$existingKey = aws ec2 describe-key-pairs --key-names $KeyName --region $Region 2>&1
if ($LASTEXITCODE -eq 0 -and $existingKey -match "KeyPairId") {
    $keyExistsInAws = $true
    if (-not (Test-Path $keyFile)) {
        Write-Host "  WARNING: Key pair exists in AWS but .pem file not found at $keyFile" -ForegroundColor Red
        Write-Host "  Deleting old key and creating new one..." -ForegroundColor Yellow
        aws ec2 delete-key-pair --key-name $KeyName --region $Region 2>$null
        $keyExistsInAws = $false
    } else {
        Write-Info "Key pair '$KeyName' already exists"
        Write-Ok "Key file exists: $keyFile"
    }
} else {
    # Key doesn't exist in AWS — remove stale local .pem if present
    if (Test-Path $keyFile) {
        Write-Info "Removing stale local key file (key no longer in AWS)..."
        Remove-Item $keyFile -Force
    }
}

if (-not $keyExistsInAws) {
    # Create .ssh directory if needed
    $sshDir = "$HOME\.ssh"
    if (-not (Test-Path $sshDir)) { New-Item -ItemType Directory -Path $sshDir -Force | Out-Null }

    $keyResult = aws ec2 create-key-pair `
        --key-name $KeyName `
        --key-type rsa `
        --region $Region `
        --query "KeyMaterial" `
        --output text 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Fail "Could not create key pair: $keyResult"
        exit 1
    }

    $keyResult | Out-File -FilePath $keyFile -Encoding ASCII -NoNewline
    Write-Ok "Key saved to: $keyFile"
}

###############################################################################
# STEP 2: Create Security Group
###############################################################################
Write-Step "Creating Security Group..."

$sgName = "${ProjectName}-sg"

# Check if SG already exists
$existingSg = aws ec2 describe-security-groups `
    --filters "Name=group-name,Values=$sgName" "Name=vpc-id,Values=$vpcId" `
    --region $Region --query "SecurityGroups[0].GroupId" --output text 2>&1

if ($LASTEXITCODE -eq 0 -and $existingSg -and $existingSg -ne "None" -and $existingSg -ne "") {
    $sgId = $existingSg
    Write-Info "Security group already exists: $sgId"
} else {
    $sgId = aws ec2 create-security-group `
        --group-name $sgName `
        --description "CRM test instance - HTTP HTTPS SSH" `
        --vpc-id $vpcId `
        --region $Region `
        --query "GroupId" `
        --output text 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Fail "Could not create security group: $sgId"
        exit 1
    }

    # Allow SSH (port 22), HTTP (80), HTTPS (443), frontend direct (3000)
    $ports = @(22, 80, 443, 3000)
    foreach ($port in $ports) {
        aws ec2 authorize-security-group-ingress `
            --group-id $sgId --protocol tcp --port $port `
            --cidr "0.0.0.0/0" --region $Region 2>$null | Out-Null
    }

    Write-Ok "Security group created: $sgId (ports: 22, 80, 443, 3000)"
}

###############################################################################
# STEP 3: Get Latest Amazon Linux 2023 AMI
###############################################################################
Write-Step "Finding latest Amazon Linux 2023 AMI..."

$amiId = aws ec2 describe-images `
    --owners amazon `
    --filters "Name=name,Values=al2023-ami-*-x86_64" "Name=state,Values=available" `
    --query "sort_by(Images, &CreationDate)[-1].ImageId" `
    --region $Region `
    --output text 2>&1

if ($LASTEXITCODE -ne 0 -or -not $amiId -or $amiId -eq "None") {
    Write-Fail "Could not find AMI: $amiId"
    exit 1
}

Write-Ok "AMI: $amiId"

###############################################################################
# STEP 4: Create User Data Script
###############################################################################
Write-Step "Preparing server setup script..."

$userData = @'
#!/bin/bash
set -e
exec > /var/log/crm-setup.log 2>&1

echo "=== CRM Setup Starting ==="

# Install Docker
dnf update -y
dnf install -y docker git
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Install Docker Compose
curl -sL "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

# Install Nginx reverse proxy
dnf install -y nginx

cat > /etc/nginx/conf.d/crm.conf << 'NGINXCONF'
server {
    listen 80;
    server_name _;

    client_max_body_size 50M;
    proxy_connect_timeout 300s;
    proxy_send_timeout    300s;
    proxy_read_timeout    300s;

    # Frontend (default)
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Backend API services
    location /api/v1/auth/         { proxy_pass http://127.0.0.1:8081; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/leads/        { proxy_pass http://127.0.0.1:8082; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/accounts/     { proxy_pass http://127.0.0.1:8083; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/contacts/     { proxy_pass http://127.0.0.1:8084; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/opportunities/ { proxy_pass http://127.0.0.1:8085; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/activities/   { proxy_pass http://127.0.0.1:8086; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/notifications/ { proxy_pass http://127.0.0.1:8087; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/workflows/    { proxy_pass http://127.0.0.1:8088; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/ai/           { proxy_pass http://127.0.0.1:8089; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/email/        { proxy_pass http://127.0.0.1:8090; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/integrations/ { proxy_pass http://127.0.0.1:8091; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/cases/        { proxy_pass http://127.0.0.1:8092; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/campaigns/    { proxy_pass http://127.0.0.1:8093; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/v1/webform/      { proxy_pass http://127.0.0.1:8091; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
    location /api/agent/           { proxy_pass http://127.0.0.1:9100; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
}
NGINXCONF

# Remove default nginx page
rm -f /etc/nginx/conf.d/default.conf 2>/dev/null || true

systemctl start nginx
systemctl enable nginx

# Auto-start CRM on reboot
cat > /etc/systemd/system/crm-app.service << 'SVC'
[Unit]
Description=CRM Application
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ec2-user/CRMSAPP
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
User=ec2-user

[Install]
WantedBy=multi-user.target
SVC
systemctl daemon-reload
systemctl enable crm-app

echo "=== CRM Setup Complete ==="
'@

# Base64 encode for AWS
$userDataB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($userData))

###############################################################################
# STEP 5: Launch Spot Instance (using run-instances with spot market options)
###############################################################################
Write-Step "Launching EC2 t3.xlarge Spot Instance..."

$runResult = aws ec2 run-instances `
    --image-id $amiId `
    --instance-type $InstanceType `
    --key-name $KeyName `
    --security-group-ids $sgId `
    --block-device-mappings "[{`"DeviceName`":`"/dev/xvda`",`"Ebs`":{`"VolumeSize`":30,`"VolumeType`":`"gp3`",`"Encrypted`":true,`"DeleteOnTermination`":true}}]" `
    --instance-market-options "MarketType=spot,SpotOptions={SpotInstanceType=persistent,InstanceInterruptionBehavior=stop}" `
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${ProjectName}-spot}]" `
    --user-data $userDataB64 `
    --count 1 `
    --region $Region `
    --output json 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Fail "Could not launch instance: $runResult"
    Write-Host ""
    Write-Host "  If you see 'not authorized', your IAM user may need:" -ForegroundColor Yellow
    Write-Host "  ec2:RunInstances, ec2:CreateTags permissions" -ForegroundColor Yellow
    exit 1
}

$instanceData = $runResult | ConvertFrom-Json
$instanceId = $instanceData.Instances[0].InstanceId

if (-not $instanceId) {
    Write-Fail "Instance ID not found in response"
    exit 1
}

Write-Ok "Instance launched: $instanceId"
Write-Info "Waiting for instance to start..."

aws ec2 wait instance-running --instance-ids $instanceId --region $Region
Write-Ok "Instance is running"

###############################################################################
# STEP 6: Allocate Elastic IP
###############################################################################
Write-Step "Allocating Elastic IP (static address for testers)..."

$eipJson = aws ec2 allocate-address `
    --domain vpc `
    --tag-specifications "ResourceType=elastic-ip,Tags=[{Key=Name,Value=${ProjectName}-eip}]" `
    --region $Region `
    --output json 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Fail "Could not allocate Elastic IP: $eipJson"
    Write-Host "  Your instance is running. Get its public IP from AWS Console." -ForegroundColor Yellow
    exit 1
}

$eip = $eipJson | ConvertFrom-Json
$allocationId = $eip.AllocationId
$publicIp = $eip.PublicIp

aws ec2 associate-address `
    --instance-id $instanceId `
    --allocation-id $allocationId `
    --region $Region 2>$null | Out-Null

Write-Ok "Elastic IP: $publicIp"

###############################################################################
# STEP 7: Save connection info
###############################################################################

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $scriptDir) { $scriptDir = "." }
$infoFile = Join-Path $scriptDir "connection-info.txt"

@"
============================================================
  CRM Test Instance - Connection Info
============================================================
  Public IP      : $publicIp
  Instance ID    : $instanceId
  EIP Allocation : $allocationId
  Key File       : $keyFile
  Region         : $Region

  SSH Command:
    ssh -i "$keyFile" ec2-user@$publicIp

  App URL (after setup):
    http://$publicIp

  Monthly Cost: ~`$20-35 (Spot)
============================================================
"@ | Out-File -FilePath $infoFile -Encoding UTF8

Write-Ok "Connection info saved to: $infoFile"

###############################################################################
# DONE — Print instructions
###############################################################################
Write-Host ""
Write-Host "========================================================" -ForegroundColor Green
Write-Host "  EC2 Spot Instance Ready!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Public IP:  $publicIp" -ForegroundColor Cyan
Write-Host "  Instance:   $instanceId" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Wait 2-3 minutes for server setup to complete, then:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  STEP 1: SSH into the instance" -ForegroundColor White
Write-Host "    ssh -i `"$keyFile`" ec2-user@$publicIp" -ForegroundColor Cyan
Write-Host ""
Write-Host "  STEP 2: Clone your repo" -ForegroundColor White
Write-Host "    git clone <your-repo-url> /home/ec2-user/CRMSAPP" -ForegroundColor Cyan
Write-Host ""
Write-Host "  STEP 3: (Optional) Set OpenAI API key" -ForegroundColor White
Write-Host "    cd /home/ec2-user/CRMSAPP" -ForegroundColor Cyan
Write-Host "    echo 'LLM_API_KEY=sk-proj-xxx' > .env" -ForegroundColor Cyan
Write-Host ""
Write-Host "  STEP 4: Build & start all services (~10 min first time)" -ForegroundColor White
Write-Host "    docker-compose build" -ForegroundColor Cyan
Write-Host "    docker-compose up -d" -ForegroundColor Cyan
Write-Host ""
Write-Host "  STEP 5: Share this URL with your 5 testers:" -ForegroundColor White
Write-Host "    http://$publicIp" -ForegroundColor Green
Write-Host ""
Write-Host "  STEP 6: (Optional) Seed demo data" -ForegroundColor White
Write-Host "    # From your local machine, update seed scripts to use:" -ForegroundColor Cyan
Write-Host "    # `$BASE = `"http://$publicIp`"" -ForegroundColor Cyan
Write-Host ""
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""
