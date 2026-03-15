###############################################################################
# teardown-ec2-spot.ps1 — Clean up all AWS resources created by setup script
#
# Usage:  .\aws\teardown-ec2-spot.ps1
###############################################################################

param(
    [string]$Region      = "us-east-1",
    [string]$ProjectName = "crm-test"
)

$ErrorActionPreference = "Continue"

Write-Host "`n=== Tearing down CRM test resources ===" -ForegroundColor Yellow

# Find Spot requests
Write-Host "Cancelling Spot requests..."
$spots = aws ec2 describe-spot-instance-requests `
    --filters "Name=tag:Name,Values=${ProjectName}-spot-request" `
    --region $Region --query "SpotInstanceRequests[].SpotInstanceRequestId" --output text 2>$null

if ($spots) {
    foreach ($s in $spots -split "`t") {
        aws ec2 cancel-spot-instance-requests --spot-instance-request-ids $s --region $Region 2>$null
        Write-Host "  Cancelled: $s" -ForegroundColor Green
    }
}

# Find and terminate instances
Write-Host "Terminating instances..."
$instances = aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=${ProjectName}-spot" "Name=instance-state-name,Values=running,pending,stopping,stopped" `
    --region $Region --query "Reservations[].Instances[].InstanceId" --output text 2>$null

if ($instances) {
    foreach ($i in $instances -split "`t") {
        aws ec2 terminate-instances --instance-ids $i --region $Region 2>$null | Out-Null
        Write-Host "  Terminated: $i" -ForegroundColor Green
    }
    Write-Host "  Waiting for termination..."
    aws ec2 wait instance-terminated --instance-ids ($instances -split "`t") --region $Region 2>$null
}

# Release Elastic IPs
Write-Host "Releasing Elastic IPs..."
$eips = aws ec2 describe-addresses `
    --filters "Name=tag:Name,Values=${ProjectName}-eip" `
    --region $Region --query "Addresses[].AllocationId" --output text 2>$null

if ($eips) {
    foreach ($e in $eips -split "`t") {
        aws ec2 release-address --allocation-id $e --region $Region 2>$null
        Write-Host "  Released: $e" -ForegroundColor Green
    }
}

# Delete Security Group
Write-Host "Deleting security group..."
$sgId = aws ec2 describe-security-groups `
    --filters "Name=group-name,Values=${ProjectName}-sg" `
    --region $Region --query "SecurityGroups[0].GroupId" --output text 2>$null

if ($sgId -and $sgId -ne "None") {
    Start-Sleep -Seconds 5  # Wait for ENIs to detach
    aws ec2 delete-security-group --group-id $sgId --region $Region 2>$null
    Write-Host "  Deleted: $sgId" -ForegroundColor Green
}

# Delete Key Pair
Write-Host "Deleting key pair..."
aws ec2 delete-key-pair --key-name "crm-test-key" --region $Region 2>$null
$keyFile = "$HOME\.ssh\crm-test-key.pem"
if (Test-Path $keyFile) {
    Remove-Item $keyFile -Force
    Write-Host "  Deleted: $keyFile" -ForegroundColor Green
}

Write-Host "`n=== Teardown complete ===" -ForegroundColor Green
Write-Host "All CRM test resources have been removed.`n" -ForegroundColor Green
