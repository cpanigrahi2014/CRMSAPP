# AWS Deployment Guide — Zero-Config AI CRM Platform

## Minimum Cost Deployment for 3 Test Users → Production Scale Path

---

## Architecture Summary

| Component | Count | Technology |
|-----------|-------|-----------|
| Java Microservices | 11 | Spring Boot 3.2.3 / Java 21 |
| Node.js Service | 1 | crm-ai-agent (Express + Prisma) |
| Frontend | 1 | React 18 + Nginx |
| Database | 1 | PostgreSQL 16 (13 databases) |
| Cache | 1 | Redis 7 |
| Message Broker | 1 | Kafka + Zookeeper |

---

## PHASE 1 — Minimum Cost (3 Test Users) ~$45–65/month

### Option A: Single EC2 Instance (RECOMMENDED — Simplest)

Run everything on **one EC2 instance** using Docker Compose — identical to your local setup.

#### Instance Selection

| Instance | vCPUs | RAM | Price (us-east-1) | Notes |
|----------|-------|-----|-------------------|-------|
| **t3.xlarge** | 4 | 16 GB | ~$0.1664/hr = **$50/mo** (on-demand) | **Recommended** — handles all 15 containers |
| t3.large | 2 | 8 GB | ~$0.0832/hr = **$25/mo** | Tight — may OOM with all services |
| t3.xlarge Spot | 4 | 16 GB | ~$0.02–0.05/hr = **$15–36/mo** | Cheapest — but can be interrupted |

> **Best pick for testing**: **t3.xlarge Spot Instance** at ~$20/mo, or **t3.xlarge Reserved** 1-year at ~$32/mo.

#### Step-by-Step Deployment

##### 1. Launch EC2 Instance

```bash
# AWS Console → EC2 → Launch Instance
# AMI: Amazon Linux 2023 (or Ubuntu 22.04 LTS)
# Instance type: t3.xlarge
# Storage: 30 GB gp3 (free tier covers 30 GB)
# Security Group: Allow ports 80, 443, 22
# Key pair: Create or select existing
```

##### 2. Connect & Install Docker

```bash
# SSH into the instance
ssh -i your-key.pem ec2-user@<public-ip>

# Amazon Linux 2023
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Re-login for docker group
exit
ssh -i your-key.pem ec2-user@<public-ip>
```

##### 3. Clone & Configure

```bash
# Clone your repo (or SCP the project)
git clone <your-repo-url> /home/ec2-user/CRMSAPP
cd /home/ec2-user/CRMSAPP

# Create environment file
cat > .env << 'EOF'
LLM_API_KEY=sk-proj-your-openai-key-here
LLM_DEFAULT_MODEL=gpt-4o
LLM_BASE_URL=https://api.openai.com/v1
EOF
```

##### 4. Build & Launch All Services

```bash
# Build all images (first time takes ~10 min)
docker compose build

# Start everything
docker compose up -d

# Watch logs
docker compose logs -f --tail=50
```

##### 5. Verify Services

```bash
# Check all containers are running
docker compose ps

# Test auth service
curl http://localhost:8081/actuator/health

# Test frontend
curl -s http://localhost:3000 | head -5
```

##### 6. Configure Security Group (AWS Console)

| Type | Port | Source | Purpose |
|------|------|--------|---------|
| SSH | 22 | Your IP only | Admin access |
| HTTP | 80 | 0.0.0.0/0 | Redirect to HTTPS |
| HTTPS | 443 | 0.0.0.0/0 | App access |
| Custom | 3000 | 0.0.0.0/0 | App access (before Nginx/ALB) |

##### 7. Add SSL with Let's Encrypt (Free)

```bash
# Install Certbot
sudo dnf install -y certbot

# Point your domain to the EC2 public IP (Route 53 or your DNS provider)
# Example: crm.yourcompany.com → A record → <EC2-public-ip>

# Install Nginx as reverse proxy on the host
sudo dnf install -y nginx

# Create Nginx config
sudo tee /etc/nginx/conf.d/crm.conf << 'EOF'
server {
    listen 80;
    server_name crm.yourcompany.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name crm.yourcompany.com;

    ssl_certificate /etc/letsencrypt/live/crm.yourcompany.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/crm.yourcompany.com/privkey.pem;

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
}
EOF

# Get SSL certificate
sudo certbot certonly --standalone -d crm.yourcompany.com --agree-tos -m your@email.com

# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Auto-renew certificate (cron)
echo "0 3 * * * certbot renew --quiet --post-hook 'systemctl reload nginx'" | sudo tee -a /var/spool/cron/ec2-user
```

##### 8. Auto-Start on Reboot

```bash
# Create systemd service
sudo tee /etc/systemd/system/crm-app.service << 'EOF'
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
EOF

sudo systemctl enable crm-app
```

#### Phase 1 Cost Breakdown

| Resource | Monthly Cost |
|----------|-------------|
| EC2 t3.xlarge (Spot) | $15–36 |
| EBS 30 GB gp3 | $2.40 |
| Elastic IP | $3.65 (free if attached to running instance) |
| Data transfer (3 users) | ~$1 |
| Route 53 hosted zone | $0.50 |
| SSL (Let's Encrypt) | Free |
| **TOTAL** | **$22–43/month** |

Or with On-Demand:

| Resource | Monthly Cost |
|----------|-------------|
| EC2 t3.xlarge (On-Demand) | $50 |
| EBS + IP + Transfer + DNS | ~$7 |
| **TOTAL** | **~$57/month** |

---

### Option B: Elastic Beanstalk + RDS (Slightly More — Better Managed)

If you want AWS to manage more for you:

| Resource | Service | Cost |
|----------|---------|------|
| App Server | Elastic Beanstalk (t3.xlarge) | $50/mo |
| Database | RDS PostgreSQL db.t3.micro (free tier) | $0 (first 12 months) |
| Cache | ElastiCache t3.micro | $12/mo |
| **TOTAL** | | **~$62/month** |

> Not recommended for testing — adds complexity without benefit for 3 users.

---

## PHASE 2 — Production Scale (50+ Users)

When you're ready to scale, migrate to this architecture:

### Architecture

```
Route 53 (DNS)
    ↓
CloudFront (CDN — serves frontend static files)
    ↓
Application Load Balancer (ALB)
    ↓
ECS Fargate (each microservice = 1 task)
    ↓
    ├── auth-service
    ├── lead-service
    ├── account-service
    ├── contact-service
    ├── opportunity-service
    ├── activity-service
    ├── notification-service
    ├── workflow-service
    ├── ai-service
    ├── email-service
    ├── integration-service
    └── ai-agent
    ↓
RDS PostgreSQL (Multi-AZ)  +  ElastiCache Redis  +  Amazon MSK (Kafka)
```

### Production Cost Estimate

| Resource | Service | Sizing | Monthly Cost |
|----------|---------|--------|-------------|
| Frontend | CloudFront + S3 | Static hosting | $5 |
| Load Balancer | ALB | 1 instance | $22 |
| App Services | ECS Fargate | 12 tasks × 0.5 vCPU / 1 GB | $120 |
| AI Agent | ECS Fargate | 1 task × 0.5 vCPU / 1 GB | $10 |
| Database | RDS PostgreSQL | db.t3.medium (Multi-AZ) | $130 |
| Cache | ElastiCache | cache.t3.small | $25 |
| Kafka | Amazon MSK | kafka.t3.small (2 brokers) | $90 |
| Secrets | Secrets Manager | 15 secrets | $6 |
| Monitoring | CloudWatch | Basic | $10 |
| NAT Gateway | VPC | 1 NAT | $32 |
| **TOTAL** | | | **~$450/month** |

---

## Migration Path: Phase 1 → Phase 2

### Step 1 — Push Images to ECR

```bash
# Create ECR repositories (one per service)
SERVICES="auth-service lead-service account-service contact-service opportunity-service activity-service notification-service workflow-service ai-service email-service integration-service ai-agent frontend"

AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1

for svc in $SERVICES; do
  aws ecr create-repository --repository-name crm/$svc --region $REGION
done

# Login to ECR
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT.dkr.ecr.$REGION.amazonaws.com

# Tag and push each image
for svc in $SERVICES; do
  docker tag crmsapp-$svc:latest $AWS_ACCOUNT.dkr.ecr.$REGION.amazonaws.com/crm/$svc:latest
  docker push $AWS_ACCOUNT.dkr.ecr.$REGION.amazonaws.com/crm/$svc:latest
done
```

### Step 2 — Create RDS PostgreSQL

```bash
aws rds create-db-instance \
  --db-instance-identifier crm-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16 \
  --master-username postgres \
  --master-user-password <strong-password-here> \
  --allocated-storage 20 \
  --storage-type gp3 \
  --vpc-security-group-ids <sg-id> \
  --db-subnet-group-name <subnet-group> \
  --no-multi-az \
  --publicly-accessible false
```

Then run `init-databases.sql` against the RDS instance.

### Step 3 — Create ECS Cluster & Task Definitions

This is automated with the Terraform/CloudFormation templates below.

---

## Quick Deploy Scripts

### deploy-ec2.sh — One-Command Deployment

```bash
#!/bin/bash
# Run this ON the EC2 instance after cloning the repo
set -e

echo "=== CRM Platform — EC2 Deployment ==="

# Check Docker
if ! command -v docker &> /dev/null; then
  echo "Installing Docker..."
  sudo dnf install -y docker
  sudo systemctl start docker
  sudo systemctl enable docker
  sudo usermod -aG docker $USER
  echo "Docker installed. Please re-login and run this script again."
  exit 0
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
  echo "Installing Docker Compose..."
  sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  sudo chmod +x /usr/local/bin/docker-compose
fi

# Check .env
if [ ! -f .env ]; then
  echo "ERROR: Create .env file with LLM_API_KEY first"
  echo 'echo "LLM_API_KEY=sk-proj-your-key" > .env'
  exit 1
fi

# Build & Start
echo "Building all services (this takes ~10 min first time)..."
docker compose build
echo "Starting all services..."
docker compose up -d

# Wait for health
echo "Waiting for services to start..."
sleep 30

echo ""
echo "=== Deployment Complete ==="
echo "Frontend: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):3000"
echo ""
docker compose ps
```

---

## Recommended AWS Free Tier Maximization

If the AWS account is new (within 12 months), you can leverage free tier:

| Service | Free Tier | Saves |
|---------|-----------|-------|
| EC2 t3.micro | 750 hrs/month | Not enough RAM — skip |
| EC2 t3.xlarge | Not free tier | Still need this |
| RDS db.t3.micro | 750 hrs/month free | $15/mo |
| ElastiCache | Not free tier | — |
| S3 | 5 GB free | $0 |
| Route 53 | — | $0.50/mo |
| Data Transfer | 100 GB/month free | $0 |

**Free tier doesn't help much here** because we need at least 16 GB RAM for all services. Stick with Option A (single EC2).

---

## Cost-Saving Tips

### Testing Phase (3 Users)

1. **Use Spot Instances** — save 60-70% on EC2
   ```bash
   # Request spot instance via console or CLI
   aws ec2 request-spot-instances \
     --instance-count 1 \
     --type "persistent" \
     --launch-specification '{
       "ImageId": "ami-0c02fb55956c7d316",
       "InstanceType": "t3.xlarge",
       "KeyName": "your-key",
       "SecurityGroupIds": ["sg-xxx"]
     }'
   ```

2. **Schedule start/stop** — if testers only work 8 hrs/day, 5 days/week:
   ```bash
   # Save 76% by running only during business hours
   # Use AWS Instance Scheduler or Lambda + EventBridge
   # Cost: $50/mo → $12/mo
   ```

3. **Use Savings Plans** — 1-year commitment = 30-40% off

4. **Reduce container memory** — Add resource limits in docker-compose:
   ```yaml
   services:
     auth-service:
       deploy:
         resources:
           limits:
             memory: 512M
           reservations:
             memory: 256M
   ```

### When Scaling to Production

1. **Use Fargate Spot** for non-critical services (reports, email, notifications) — 70% off
2. **RDS Reserved Instances** — 1-year = 30% off, 3-year = 50% off
3. **CloudFront for frontend** — cheaper than serving from EC2 and faster globally
4. **Graviton (ARM) instances** — 20% cheaper than Intel (t4g vs t3)

---

## Domain & SSL Setup (With Route 53)

```bash
# 1. Register domain or use existing (Route 53 = $12/year for .com)

# 2. Create hosted zone
aws route53 create-hosted-zone --name yourcrm.com --caller-reference $(date +%s)

# 3. Create A record pointing to EC2
aws route53 change-resource-record-sets --hosted-zone-id <zone-id> --change-batch '{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "crm.yourcrm.com",
      "Type": "A",
      "TTL": 300,
      "ResourceRecords": [{"Value": "<ec2-public-ip>"}]
    }
  }]
}'

# 4. SSL via Let's Encrypt (as shown in Step 7 above)
```

---

## Monitoring & Backups

### Basic Monitoring (Free)

```bash
# Install CloudWatch agent on EC2
sudo dnf install -y amazon-cloudwatch-agent

# Monitor disk, memory, and Docker
# Set up CloudWatch Alarms:
# - CPU > 80% → Email alert
# - Disk > 85% → Email alert
# - Instance status check failed → Auto-reboot
```

### Database Backups

```bash
# Daily automated backup via cron
# Runs at 2 AM, keeps last 7 backups
cat >> /var/spool/cron/ec2-user << 'EOF'
0 2 * * * cd /home/ec2-user/CRMSAPP && docker exec crm-postgres pg_dumpall -U postgres | gzip > /home/ec2-user/backups/crm-backup-$(date +\%Y\%m\%d).sql.gz && find /home/ec2-user/backups -name "*.sql.gz" -mtime +7 -delete
EOF

mkdir -p /home/ec2-user/backups

# Optional: Upload to S3 (pennies/month)
# aws s3 cp /home/ec2-user/backups/ s3://your-crm-backups/ --recursive
```

---

## Quick Comparison

| Aspect | Phase 1 (EC2) | Phase 2 (ECS/Fargate) |
|--------|---------------|----------------------|
| **Users** | 1–10 | 50–10,000+ |
| **Cost** | $22–57/mo | $450+/mo |
| **Deploy Time** | 30 min | 2–4 hours |
| **Scaling** | Manual (resize EC2) | Auto-scaling |
| **Availability** | Single AZ | Multi-AZ |
| **Recovery** | Manual restart | Auto-heal |
| **SSL** | Let's Encrypt | ACM (free, auto-renew) |
| **Monitoring** | Basic CloudWatch | Full CloudWatch + X-Ray |
| **Database** | Docker PostgreSQL | RDS (managed, backups, failover) |
| **Maintenance** | You manage everything | AWS manages infra |

---

## TL;DR — Fastest Path for 3 Test Users

```bash
# 1. Launch EC2 t3.xlarge (us-east-1) with Amazon Linux 2023, 30 GB gp3
# 2. SSH in and run:

sudo dnf install -y docker git
sudo systemctl start docker && sudo systemctl enable docker
sudo usermod -aG docker $USER && exit

# Re-login, then:
git clone <your-repo> ~/CRMSAPP && cd ~/CRMSAPP
echo "LLM_API_KEY=sk-proj-your-key" > .env
docker compose build && docker compose up -d

# 3. Access: http://<EC2-Public-IP>:3000
# 4. Login: sarah.chen@acmecorp.com / Demo@2026!
# 5. Total cost: ~$50/month (on-demand) or ~$20/month (spot)
```

When ready for production → push images to ECR → deploy ECS Fargate + RDS + MSK.

---

*Last updated: March 2026*
