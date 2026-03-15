###############################################################################
# CRM Platform — Minimum Cost EC2 Deployment for Testing (5 Users)
# Single EC2 Spot Instance + Docker Compose
# Estimated cost: ~$20-35/month
###############################################################################

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "crm-platform"
      Environment = "test"
      ManagedBy   = "terraform"
    }
  }
}

# ── Variables ────────────────────────────────────────────────────────────────

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "key_pair_name" {
  description = "Name of an existing EC2 key pair for SSH access"
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed for SSH (your IP/32)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "llm_api_key" {
  description = "OpenAI API key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "use_spot" {
  description = "Use Spot instance for ~60% cost savings (may be interrupted)"
  type        = bool
  default     = true
}

# ── VPC (default) ───────────────────────────────────────────────────────────

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

# ── Security Group ──────────────────────────────────────────────────────────

resource "aws_security_group" "crm" {
  name_prefix = "crm-test-"
  description = "CRM test instance"
  vpc_id      = data.aws_vpc.default.id

  # SSH
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  # HTTP (frontend)
  ingress {
    description = "Frontend"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS
  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # App port (direct access fallback)
  ingress {
    description = "Frontend direct"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle { create_before_destroy = true }
}

# ── Latest Amazon Linux 2023 AMI ────────────────────────────────────────────

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ── User Data (install Docker, clone repo, start services) ──────────────────

locals {
  user_data = <<-EOF
    #!/bin/bash
    set -e

    # Install Docker
    dnf update -y
    dnf install -y docker git
    systemctl start docker
    systemctl enable docker
    usermod -aG docker ec2-user

    # Install Docker Compose
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
      -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

    # Install Nginx for reverse proxy
    dnf install -y nginx
    cat > /etc/nginx/conf.d/crm.conf << 'NGINX'
    server {
        listen 80;
        server_name _;

        # Frontend
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

        # API routing
        location /api/v1/auth   { proxy_pass http://127.0.0.1:8081; proxy_set_header Host $host; }
        location /api/v1/leads  { proxy_pass http://127.0.0.1:8082; proxy_set_header Host $host; }
        location /api/v1/accounts { proxy_pass http://127.0.0.1:8083; proxy_set_header Host $host; }
        location /api/v1/contacts { proxy_pass http://127.0.0.1:8084; proxy_set_header Host $host; }
        location /api/v1/opportunities { proxy_pass http://127.0.0.1:8085; proxy_set_header Host $host; }
        location /api/v1/activities { proxy_pass http://127.0.0.1:8086; proxy_set_header Host $host; }
        location /api/v1/notifications { proxy_pass http://127.0.0.1:8087; proxy_set_header Host $host; }
        location /api/v1/workflows { proxy_pass http://127.0.0.1:8088; proxy_set_header Host $host; }
        location /api/v1/ai     { proxy_pass http://127.0.0.1:8089; proxy_set_header Host $host; }
        location /api/v1/email  { proxy_pass http://127.0.0.1:8090; proxy_set_header Host $host; }
        location /api/v1/integrations { proxy_pass http://127.0.0.1:8091; proxy_set_header Host $host; }
        location /api/v1/cases  { proxy_pass http://127.0.0.1:8092; proxy_set_header Host $host; }
        location /api/v1/campaigns { proxy_pass http://127.0.0.1:8093; proxy_set_header Host $host; }
        location /api/agent     { proxy_pass http://127.0.0.1:9100; proxy_set_header Host $host; }
    }
    NGINX
    systemctl start nginx
    systemctl enable nginx

    # Create .env for LLM key
    mkdir -p /home/ec2-user/crm
    cat > /home/ec2-user/crm/.env << 'ENVFILE'
    LLM_API_KEY=${var.llm_api_key}
    LLM_DEFAULT_MODEL=gpt-4o
    LLM_BASE_URL=https://api.openai.com/v1
    ENVFILE
    chown ec2-user:ec2-user /home/ec2-user/crm/.env

    # Auto-start script (user will clone repo and run)
    cat > /home/ec2-user/start-crm.sh << 'SCRIPT'
    #!/bin/bash
    cd /home/ec2-user/crm
    if [ ! -f docker-compose.yml ]; then
      echo "Clone your repo first:"
      echo "  git clone <your-repo-url> /home/ec2-user/crm"
      exit 1
    fi
    cp .env.bak .env 2>/dev/null || true
    docker-compose build
    docker-compose up -d
    echo "CRM started! Access at http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)"
    SCRIPT
    chmod +x /home/ec2-user/start-crm.sh
    chown ec2-user:ec2-user /home/ec2-user/start-crm.sh

    # Systemd auto-start on reboot
    cat > /etc/systemd/system/crm-app.service << 'SVC'
    [Unit]
    Description=CRM Application
    After=docker.service
    Requires=docker.service

    [Service]
    Type=oneshot
    RemainAfterExit=yes
    WorkingDirectory=/home/ec2-user/crm
    ExecStart=/usr/local/bin/docker-compose up -d
    ExecStop=/usr/local/bin/docker-compose down
    User=ec2-user

    [Install]
    WantedBy=multi-user.target
    SVC
    systemctl daemon-reload
    systemctl enable crm-app
  EOF
}

# ── EC2 Instance (On-Demand) ────────────────────────────────────────────────

resource "aws_instance" "crm" {
  count = var.use_spot ? 0 : 1

  ami                    = data.aws_ami.al2023.id
  instance_type          = "t3.xlarge"
  key_name               = var.key_pair_name
  vpc_security_group_ids = [aws_security_group.crm.id]
  subnet_id              = data.aws_subnets.default.ids[0]

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = base64encode(local.user_data)

  tags = { Name = "crm-test-instance" }
}

# ── EC2 Spot Instance (cheaper) ─────────────────────────────────────────────

resource "aws_spot_instance_request" "crm" {
  count = var.use_spot ? 1 : 0

  ami                    = data.aws_ami.al2023.id
  instance_type          = "t3.xlarge"
  key_name               = var.key_pair_name
  vpc_security_group_ids = [aws_security_group.crm.id]
  subnet_id              = data.aws_subnets.default.ids[0]

  spot_type            = "persistent"
  wait_for_fulfillment = true

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = base64encode(local.user_data)

  tags = { Name = "crm-test-spot" }
}

# ── Elastic IP (static IP for testers) ──────────────────────────────────────

resource "aws_eip" "crm" {
  instance = var.use_spot ? aws_spot_instance_request.crm[0].spot_instance_id : aws_instance.crm[0].id
  domain   = "vpc"
  tags     = { Name = "crm-test-eip" }
}

# ── Outputs ──────────────────────────────────────────────────────────────────

output "app_url" {
  value = "http://${aws_eip.crm.public_ip}"
}

output "ssh_command" {
  value = "ssh -i ${var.key_pair_name}.pem ec2-user@${aws_eip.crm.public_ip}"
}

output "setup_instructions" {
  value = <<-EOT

    ╔══════════════════════════════════════════════════════════╗
    ║  CRM Test Instance Ready!                                ║
    ╠══════════════════════════════════════════════════════════╣
    ║                                                          ║
    ║  1. SSH in:                                              ║
    ║     ssh -i ${var.key_pair_name}.pem ec2-user@${aws_eip.crm.public_ip}
    ║                                                          ║
    ║  2. Clone your repo:                                     ║
    ║     git clone <repo-url> /home/ec2-user/crm              ║
    ║                                                          ║
    ║  3. Start CRM:                                           ║
    ║     cd /home/ec2-user/crm                                ║
    ║     docker-compose up -d                                 ║
    ║                                                          ║
    ║  4. Share this URL with testers:                         ║
    ║     http://${aws_eip.crm.public_ip}
    ║                                                          ║
    ║  Monthly cost: ~$20-35 (Spot) or ~$55 (On-Demand)       ║
    ╚══════════════════════════════════════════════════════════╝
  EOT
}
