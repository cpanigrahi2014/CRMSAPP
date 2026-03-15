###############################################################################
# ECS Cluster, Cloud Map, Task Definitions & Services
###############################################################################

# ── Cluster ──────────────────────────────────────────────────────────────────

resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = { Name = "${var.project_name}-${var.environment}-cluster" }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
    base              = 1
  }
}

# ── Cloud Map Service Discovery ──────────────────────────────────────────────

resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "${var.project_name}.local"
  description = "Service discovery for CRM services"
  vpc         = aws_vpc.main.id
}

resource "aws_service_discovery_service" "backend" {
  for_each = local.backend_services

  name = each.key

  dns_config {
    namespace_id   = aws_service_discovery_private_dns_namespace.main.id
    routing_policy = "MULTIVALUE"

    dns_records {
      ttl  = 10
      type = "A"
    }
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_service_discovery_service" "frontend" {
  name = "frontend"

  dns_config {
    namespace_id   = aws_service_discovery_private_dns_namespace.main.id
    routing_policy = "MULTIVALUE"

    dns_records {
      ttl  = 10
      type = "A"
    }
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

# ── CloudWatch Log Groups ───────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "services" {
  for_each = toset(concat(local.services))

  name              = "/ecs/${var.project_name}-${var.environment}/${each.key}"
  retention_in_days = 30
}

# ── Shared infrastructure endpoints ─────────────────────────────────────────

locals {
  db_host     = aws_db_instance.main.address
  db_port     = tostring(aws_db_instance.main.port)
  redis_host  = aws_elasticache_replication_group.main.primary_endpoint_address
  redis_port  = "6379"

  # MSK bootstrap brokers
  kafka_servers = aws_msk_cluster.main.bootstrap_brokers

  # Common env vars for all Java services
  common_env = [
    { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
    { name = "DB_HOST",     value = local.db_host },
    { name = "DB_PORT",     value = local.db_port },
    { name = "DB_USERNAME", value = var.db_master_username },
    { name = "REDIS_HOST",  value = local.redis_host },
    { name = "REDIS_PORT",  value = local.redis_port },
    { name = "KAFKA_SERVERS", value = local.kafka_servers },
    { name = "MANAGEMENT_HEALTH_MAIL_ENABLED", value = "false" },
  ]

  # Secrets from Secrets Manager
  common_secrets = [
    { name = "DB_PASSWORD",  valueFrom = aws_secretsmanager_secret.db_password.arn },
    { name = "JWT_SECRET",   valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
  ]

  ecr_prefix = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com/${var.project_name}"

  # Per-service database mappings
  service_databases = {
    "auth-service"         = "crm_auth"
    "lead-service"         = "crm_leads"
    "account-service"      = "crm_accounts"
    "contact-service"      = "crm_contacts"
    "opportunity-service"  = "crm_opportunities"
    "activity-service"     = "crm_activities"
    "notification-service" = "crm_notifications"
    "workflow-service"     = "crm_workflows"
    "ai-service"           = "crm_ai"
    "email-service"        = "crm_email"
    "integration-service"  = "crm_integrations"
    "case-service"         = "crm_cases"
    "campaign-service"     = "crm_campaigns"
  }

  # Extra env vars per service
  service_extra_env = {
    "auth-service" = [
      { name = "SERVER_PORT", value = "8081" },
      { name = "FRONTEND_URL", value = "https://${var.domain_name != "" ? var.domain_name : aws_lb.main.dns_name}" },
    ]
    "lead-service" = [
      { name = "SERVER_PORT", value = "8082" },
      { name = "OPPORTUNITY_SERVICE_URL", value = "http://opportunity-service.${var.project_name}.local:8085" },
      { name = "ACCOUNT_SERVICE_URL",     value = "http://account-service.${var.project_name}.local:8083" },
      { name = "CONTACT_SERVICE_URL",     value = "http://contact-service.${var.project_name}.local:8084" },
      { name = "ACTIVITY_SERVICE_URL",    value = "http://activity-service.${var.project_name}.local:8086" },
    ]
    "account-service" = [
      { name = "SERVER_PORT", value = "8083" },
      { name = "CONTACT_SERVICE_URL",     value = "http://contact-service.${var.project_name}.local:8084" },
      { name = "OPPORTUNITY_SERVICE_URL", value = "http://opportunity-service.${var.project_name}.local:8085" },
    ]
    "contact-service" = [
      { name = "SERVER_PORT", value = "8084" },
    ]
    "opportunity-service" = [
      { name = "SERVER_PORT", value = "8085" },
    ]
    "activity-service" = [
      { name = "SERVER_PORT", value = "8086" },
    ]
    "notification-service" = [
      { name = "SERVER_PORT", value = "8087" },
      { name = "MAIL_HOST",     value = var.mail_host },
      { name = "MAIL_PORT",     value = tostring(var.mail_port) },
      { name = "MAIL_USERNAME", value = var.mail_username },
    ]
    "workflow-service" = [
      { name = "SERVER_PORT", value = "8088" },
    ]
    "ai-service" = [
      { name = "SERVER_PORT", value = "8089" },
      { name = "LLM_BASE_URL",      value = var.llm_base_url },
      { name = "LLM_DEFAULT_MODEL", value = var.llm_model },
    ]
    "email-service" = [
      { name = "SERVER_PORT", value = "8090" },
      { name = "MAIL_HOST",     value = var.mail_host },
      { name = "MAIL_PORT",     value = tostring(var.mail_port) },
      { name = "MAIL_USERNAME", value = var.mail_username },
    ]
    "integration-service" = [
      { name = "SERVER_PORT", value = "8091" },
      { name = "LEAD_SERVICE_URL",        value = "http://lead-service.${var.project_name}.local:8082" },
      { name = "CASE_SERVICE_URL",        value = "http://case-service.${var.project_name}.local:9093" },
      { name = "OPPORTUNITY_SERVICE_URL", value = "http://opportunity-service.${var.project_name}.local:8085" },
      { name = "ACTIVITY_SERVICE_URL",    value = "http://activity-service.${var.project_name}.local:8086" },
    ]
    "case-service" = [
      { name = "SERVER_PORT", value = "9093" },
      { name = "ESCALATION_HOURS", value = "4" },
    ]
    "campaign-service" = [
      { name = "SERVER_PORT", value = "9094" },
      { name = "OPPORTUNITY_SERVICE_URL", value = "http://opportunity-service.${var.project_name}.local:8085" },
    ]
  }

  # Extra secrets per service
  service_extra_secrets = {
    "ai-service" = [
      { name = "LLM_API_KEY", valueFrom = aws_secretsmanager_secret.llm_api_key.arn },
    ]
    "notification-service" = [
      { name = "MAIL_PASSWORD", valueFrom = aws_secretsmanager_secret.mail_password.arn },
    ]
    "email-service" = [
      { name = "MAIL_PASSWORD", valueFrom = aws_secretsmanager_secret.mail_password.arn },
    ]
  }
}

# ── Backend Task Definitions ────────────────────────────────────────────────

resource "aws_ecs_task_definition" "backend" {
  for_each = local.backend_services

  family                   = "${var.project_name}-${var.environment}-${each.key}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = each.key == "ai-agent" ? var.service_cpu : var.service_cpu
  memory                   = each.key == "ai-agent" ? var.service_memory : var.service_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = each.key
      image     = "${local.ecr_prefix}/${each.key}:latest"
      essential = true

      portMappings = [{
        containerPort = each.value.port
        protocol      = "tcp"
      }]

      environment = each.key == "ai-agent" ? [
        # AI Agent (Node.js) has different env vars
        { name = "NODE_ENV",       value = "production" },
        { name = "PORT",           value = "9100" },
        { name = "DATABASE_URL",   value = "postgresql://${var.db_master_username}@${local.db_host}:${local.db_port}/crm_ai_agent" },
        { name = "OPENAI_MODEL",   value = var.llm_model },
        { name = "AI_CONFIRMATION_MODE", value = "true" },
        { name = "CRM_AUTH_URL",         value = "http://auth-service.${var.project_name}.local:8081" },
        { name = "CRM_LEAD_URL",         value = "http://lead-service.${var.project_name}.local:8082" },
        { name = "CRM_ACCOUNT_URL",      value = "http://account-service.${var.project_name}.local:8083" },
        { name = "CRM_CONTACT_URL",      value = "http://contact-service.${var.project_name}.local:8084" },
        { name = "CRM_OPPORTUNITY_URL",  value = "http://opportunity-service.${var.project_name}.local:8085" },
        { name = "CRM_ACTIVITY_URL",     value = "http://activity-service.${var.project_name}.local:8086" },
        { name = "CRM_NOTIFICATION_URL", value = "http://notification-service.${var.project_name}.local:8087" },
        { name = "CRM_WORKFLOW_URL",     value = "http://workflow-service.${var.project_name}.local:8088" },
      ] : concat(
        local.common_env,
        [{ name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${local.db_host}:${local.db_port}/${local.service_databases[each.key]}" }],
        lookup(local.service_extra_env, each.key, []),
      )

      secrets = each.key == "ai-agent" ? [
        { name = "OPENAI_API_KEY", valueFrom = aws_secretsmanager_secret.llm_api_key.arn },
        { name = "JWT_SECRET",     valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
        { name = "DB_PASSWORD",    valueFrom = aws_secretsmanager_secret.db_password.arn },
      ] : concat(
        local.common_secrets,
        lookup(local.service_extra_secrets, each.key, []),
      )

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/${var.project_name}-${var.environment}/${each.key}"
          "awslogs-region"        = data.aws_region.current.name
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = each.key == "ai-agent" ? {
        command     = ["CMD-SHELL", "curl -f http://localhost:9100/health || exit 1"]
        interval    = 30
        timeout     = 10
        retries     = 3
        startPeriod = 60
      } : {
        command     = ["CMD-SHELL", "curl -f http://localhost:${each.value.port}/actuator/health || exit 1"]
        interval    = 30
        timeout     = 10
        retries     = 3
        startPeriod = 120
      }
    }
  ])

  tags = { Name = "${var.project_name}-${var.environment}-${each.key}" }
}

# ── Frontend Task Definition ────────────────────────────────────────────────

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${var.project_name}-${var.environment}-frontend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.frontend_cpu
  memory                   = var.frontend_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "frontend"
      image     = "${local.ecr_prefix}/frontend:latest"
      essential = true

      portMappings = [{
        containerPort = 80
        protocol      = "tcp"
      }]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/${var.project_name}-${var.environment}/frontend"
          "awslogs-region"        = data.aws_region.current.name
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost/ || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 30
      }
    }
  ])

  tags = { Name = "${var.project_name}-${var.environment}-frontend" }
}

# ── Backend ECS Services ────────────────────────────────────────────────────

resource "aws_ecs_service" "backend" {
  for_each = local.backend_services

  name            = each.key
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend[each.key].arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend[each.key].arn
    container_name   = each.key
    container_port   = each.value.port
  }

  service_registries {
    registry_arn = aws_service_discovery_service.backend[each.key].arn
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100

  tags = { Name = "${var.project_name}-${var.environment}-${each.key}" }

  depends_on = [aws_lb_listener.http]
}

# ── Frontend ECS Service ────────────────────────────────────────────────────

resource "aws_ecs_service" "frontend" {
  name            = "frontend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.frontend.arn
    container_name   = "frontend"
    container_port   = 80
  }

  service_registries {
    registry_arn = aws_service_discovery_service.frontend.arn
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = { Name = "${var.project_name}-${var.environment}-frontend" }

  depends_on = [aws_lb_listener.http]
}
