###############################################################################
# CRM Platform — AWS Infrastructure (Terraform)
# ECS Fargate + RDS PostgreSQL + ElastiCache Redis + MSK Kafka
###############################################################################

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Uncomment to use S3 backend for remote state
  # backend "s3" {
  #   bucket         = "crm-terraform-state"
  #   key            = "crm-platform/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "crm-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "crm-platform"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
