###############################################################################
# Variables
###############################################################################

variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "crm"
}

# ── VPC ──────────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

# ── RDS ──────────────────────────────────────────────────────────────────────

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 20
}

variable "db_master_username" {
  description = "RDS master username"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_master_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

variable "db_multi_az" {
  description = "Enable Multi-AZ for RDS"
  type        = bool
  default     = false
}

# ── ElastiCache ──────────────────────────────────────────────────────────────

variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.t3.small"
}

# ── MSK ──────────────────────────────────────────────────────────────────────

variable "kafka_instance_type" {
  description = "MSK broker instance type"
  type        = string
  default     = "kafka.t3.small"
}

variable "kafka_broker_count" {
  description = "Number of MSK broker nodes"
  type        = number
  default     = 2
}

# ── ECS ──────────────────────────────────────────────────────────────────────

variable "service_cpu" {
  description = "Default Fargate CPU units per service (256 = 0.25 vCPU)"
  type        = number
  default     = 512
}

variable "service_memory" {
  description = "Default Fargate memory (MiB) per service"
  type        = number
  default     = 1024
}

variable "frontend_cpu" {
  description = "Fargate CPU units for frontend"
  type        = number
  default     = 256
}

variable "frontend_memory" {
  description = "Fargate memory (MiB) for frontend"
  type        = number
  default     = 512
}

variable "desired_count" {
  description = "Desired number of ECS tasks per service"
  type        = number
  default     = 1
}

# ── Domain ───────────────────────────────────────────────────────────────────

variable "domain_name" {
  description = "Domain name for the application (leave empty to skip Route53/ACM)"
  type        = string
  default     = ""
}

# ── Secrets ──────────────────────────────────────────────────────────────────

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}

variable "llm_api_key" {
  description = "OpenAI / LLM API key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "llm_model" {
  description = "Default LLM model"
  type        = string
  default     = "gpt-4o"
}

variable "llm_base_url" {
  description = "LLM API base URL"
  type        = string
  default     = "https://api.openai.com/v1"
}

variable "mail_host" {
  description = "SMTP mail host"
  type        = string
  default     = "smtp.gmail.com"
}

variable "mail_port" {
  description = "SMTP mail port"
  type        = number
  default     = 587
}

variable "mail_username" {
  description = "SMTP mail username"
  type        = string
  default     = "noreply@example.com"
}

variable "mail_password" {
  description = "SMTP mail password"
  type        = string
  sensitive   = true
  default     = ""
}
