###############################################################################
# Application Load Balancer
###############################################################################

resource "aws_lb" "main" {
  name               = "${var.project_name}-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  enable_deletion_protection = var.environment == "prod"
  drop_invalid_header_fields = true

  tags = { Name = "${var.project_name}-${var.environment}-alb" }
}

# ── HTTP Listener (redirect to HTTPS or serve directly) ─────────────────────

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }
}

# ── Target Groups ────────────────────────────────────────────────────────────

# Service port mapping
locals {
  backend_services = {
    "auth-service"         = { port = 8081, path = "/api/auth/health",         priority = 10 }
    "lead-service"         = { port = 8082, path = "/api/leads/health",        priority = 11 }
    "account-service"      = { port = 8083, path = "/api/accounts/health",     priority = 12 }
    "contact-service"      = { port = 8084, path = "/api/contacts/health",     priority = 13 }
    "opportunity-service"  = { port = 8085, path = "/api/opportunities/health", priority = 14 }
    "activity-service"     = { port = 8086, path = "/api/activities/health",   priority = 15 }
    "notification-service" = { port = 8087, path = "/api/notifications/health", priority = 16 }
    "workflow-service"     = { port = 8088, path = "/api/workflows/health",    priority = 17 }
    "ai-service"           = { port = 8089, path = "/api/ai/health",          priority = 18 }
    "email-service"        = { port = 8090, path = "/api/email/health",       priority = 19 }
    "integration-service"  = { port = 8091, path = "/api/integrations/health", priority = 20 }
    "case-service"         = { port = 9093, path = "/api/cases/health",       priority = 21 }
    "campaign-service"     = { port = 9094, path = "/api/campaigns/health",   priority = 22 }
    "ai-agent"             = { port = 9100, path = "/health",                 priority = 23 }
  }

  # Path-prefix routing rules
  service_path_prefixes = {
    "auth-service"         = ["/api/auth/*"]
    "lead-service"         = ["/api/leads/*"]
    "account-service"      = ["/api/accounts/*"]
    "contact-service"      = ["/api/contacts/*"]
    "opportunity-service"  = ["/api/opportunities/*"]
    "activity-service"     = ["/api/activities/*"]
    "notification-service" = ["/api/notifications/*"]
    "workflow-service"     = ["/api/workflows/*"]
    "ai-service"           = ["/api/ai/*"]
    "email-service"        = ["/api/email/*"]
    "integration-service"  = ["/api/integrations/*", "/api/webform/*"]
    "case-service"         = ["/api/cases/*"]
    "campaign-service"     = ["/api/campaigns/*"]
    "ai-agent"             = ["/api/agent/*", "/agent/*"]
  }
}

# Frontend target group
resource "aws_lb_target_group" "frontend" {
  name        = "${var.project_name}-${var.environment}-frontend"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  tags = { Name = "${var.project_name}-${var.environment}-frontend-tg" }
}

# Backend target groups
resource "aws_lb_target_group" "backend" {
  for_each = local.backend_services

  name        = "${var.project_name}-${var.environment}-${substr(each.key, 0, min(length(each.key), 20))}"
  port        = each.value.port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
    matcher             = "200"
  }

  tags = { Name = "${var.project_name}-${var.environment}-${each.key}-tg" }
}

# Path-based routing rules
resource "aws_lb_listener_rule" "backend" {
  for_each = local.service_path_prefixes

  listener_arn = aws_lb_listener.http.arn
  priority     = local.backend_services[each.key].priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend[each.key].arn
  }

  condition {
    path_pattern {
      values = each.value
    }
  }
}
