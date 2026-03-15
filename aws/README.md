# AWS Deployment — CRM Platform

Production-grade AWS deployment using **ECS Fargate**, **RDS PostgreSQL**, **ElastiCache Redis**, and **Amazon MSK (Kafka)**.

## Architecture

```
       Internet
          │
     Route 53 (DNS)
          │
   Application Load Balancer
          │
    ┌─────┴──────────────────────────────────────┐
    │              ECS Fargate Cluster             │
    │                                              │
    │  ┌────────────┐  ┌────────────┐  ┌────────┐│
    │  │auth-service │  │lead-service │  │frontend││
    │  └────────────┘  └────────────┘  └────────┘│
    │  ┌────────────┐  ┌────────────┐  ┌────────┐│
    │  │account-svc  │  │contact-svc  │  │oppty   ││
    │  └────────────┘  └────────────┘  └────────┘│
    │  ┌────────────┐  ┌────────────┐  ┌────────┐│
    │  │activity-svc │  │notif-svc   │  │wf-svc  ││
    │  └────────────┘  └────────────┘  └────────┘│
    │  ┌────────────┐  ┌────────────┐  ┌────────┐│
    │  │ai-service   │  │email-svc   │  │integ   ││
    │  └────────────┘  └────────────┘  └────────┘│
    │  ┌────────────┐  ┌────────────┐  ┌────────┐│
    │  │case-service │  │campaign-svc│  │ai-agent││
    │  └────────────┘  └────────────┘  └────────┘│
    └──────────────────────────────────────────────┘
          │                │                │
    ┌─────┴───┐    ┌──────┴──────┐   ┌────┴────┐
    │   RDS    │    │ ElastiCache  │   │  MSK    │
    │PostgreSQL│    │   Redis      │   │ Kafka   │
    │(14 DBs)  │    │              │   │         │
    └──────────┘    └──────────────┘   └─────────┘
```

## Prerequisites

- **AWS CLI** v2 configured (`aws configure`)
- **Terraform** >= 1.5
- **Docker** installed and running
- AWS account with permissions for ECS, ECR, RDS, ElastiCache, MSK, VPC, IAM

## Quick Start

### 1. Configure variables

```bash
cd aws/terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values (passwords, API keys, etc.)
```

### 2. Deploy everything

**Linux/macOS:**
```bash
cd aws/scripts
chmod +x *.sh
./deploy.sh
```

**Windows (PowerShell):**
```powershell
cd aws\scripts
.\deploy.ps1
```

### 3. Access the application

After deployment, Terraform outputs the ALB DNS name:
```bash
cd aws/terraform
terraform output app_url
# → http://crm-prod-alb-123456.us-east-1.elb.amazonaws.com
```

## Deployment Options

### Full deploy
```bash
./deploy.sh                         # Linux
.\deploy.ps1                        # Windows
```

### Update single service (skip infra)
```bash
./deploy.sh --skip-infra --service auth-service
.\deploy.ps1 -SkipInfra -Service auth-service
```

### Only update ECS (images already pushed)
```bash
./deploy.sh --skip-infra --skip-build
.\deploy.ps1 -SkipInfra -SkipBuild
```

## File Structure

```
aws/
├── terraform/
│   ├── main.tf                  # Provider & backend config
│   ├── variables.tf             # Input variables
│   ├── outputs.tf               # Output values
│   ├── vpc.tf                   # VPC, subnets, NAT gateway
│   ├── security-groups.tf       # Security groups for ALB, ECS, RDS, Redis, MSK
│   ├── rds.tf                   # RDS PostgreSQL (14 databases)
│   ├── elasticache.tf           # ElastiCache Redis cluster
│   ├── msk.tf                   # Amazon MSK (Kafka)
│   ├── ecr.tf                   # ECR repositories (15 services)
│   ├── iam.tf                   # IAM roles for ECS execution & tasks
│   ├── secrets.tf               # AWS Secrets Manager
│   ├── alb.tf                   # ALB with path-based routing
│   ├── ecs.tf                   # ECS cluster, task defs, services
│   └── terraform.tfvars.example # Example config
├── scripts/
│   ├── deploy.sh                # Full deployment (Linux/macOS)
│   ├── deploy.ps1               # Full deployment (Windows)
│   ├── build-and-push.sh        # Build & push images to ECR
│   └── init-rds.sh              # Initialize RDS databases
└── README.md                    # This file
```

## Infrastructure Details

| Component | AWS Service | Default Sizing | Est. Cost/mo |
|-----------|-------------|----------------|-------------|
| 14 Backend Services + 1 Frontend | ECS Fargate | 0.5 vCPU / 1 GB each | ~$130 |
| Database | RDS PostgreSQL 16 | db.t3.medium | ~$65 |
| Cache | ElastiCache Redis 7 | cache.t3.small | ~$25 |
| Message Broker | Amazon MSK | kafka.t3.small × 2 | ~$90 |
| Load Balancer | ALB | 1 instance | ~$22 |
| NAT Gateway | VPC | 1 NAT | ~$32 |
| Secrets | Secrets Manager | 4 secrets | ~$2 |
| Logs | CloudWatch | 30 days retention | ~$10 |
| **Total** | | | **~$376/mo** |

## Service Routing (ALB)

| Path Pattern | Target Service | Port |
|-------------|---------------|------|
| `/api/auth/*` | auth-service | 8081 |
| `/api/leads/*` | lead-service | 8082 |
| `/api/accounts/*` | account-service | 8083 |
| `/api/contacts/*` | contact-service | 8084 |
| `/api/opportunities/*` | opportunity-service | 8085 |
| `/api/activities/*` | activity-service | 8086 |
| `/api/notifications/*` | notification-service | 8087 |
| `/api/workflows/*` | workflow-service | 8088 |
| `/api/ai/*` | ai-service | 8089 |
| `/api/email/*` | email-service | 8090 |
| `/api/integrations/*` | integration-service | 8091 |
| `/api/cases/*` | case-service | 9093 |
| `/api/campaigns/*` | campaign-service | 9094 |
| `/api/agent/*` | ai-agent | 9100 |
| `/*` (default) | frontend | 80 |

## Service Discovery

Services communicate via **AWS Cloud Map** private DNS:
- `auth-service.crm.local:8081`
- `lead-service.crm.local:8082`
- etc.

## Security

- All secrets stored in **AWS Secrets Manager** (DB password, JWT secret, API keys)
- RDS and Redis in **private subnets** with no public access
- ECS tasks in **private subnets** behind NAT gateway
- ALB is the only public-facing component
- Security groups restrict traffic to minimum required ports
- ECR image scanning enabled on push

## CI/CD (GitHub Actions)

The pipeline at `.github/workflows/deploy.yml`:

1. Detects which services changed (by folder)
2. Builds only changed Docker images
3. Pushes to ECR
4. Forces new ECS deployment
5. Waits for service stability

**Setup:**
1. Create an IAM OIDC identity provider for GitHub Actions
2. Create a deploy role with ECR + ECS permissions
3. Add `AWS_DEPLOY_ROLE_ARN` as a GitHub repository secret

## Scaling

Adjust in `terraform.tfvars`:

```hcl
# Scale horizontally
desired_count = 2    # 2 tasks per service

# Scale vertically
service_cpu    = 1024  # 1 vCPU
service_memory = 2048  # 2 GB

# Production database
db_instance_class = "db.t3.large"
db_multi_az       = true
```

## Cost Optimization

- Set `desired_count = 0` for unused services
- Use **Fargate Spot** for non-critical services (edit `ecs.tf`)
- Use **RDS Reserved Instances** for 30-50% savings
- Schedule start/stop with AWS Instance Scheduler
- Use **Graviton** (ARM) Fargate tasks for 20% savings
