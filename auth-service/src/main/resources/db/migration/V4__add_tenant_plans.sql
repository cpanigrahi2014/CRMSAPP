-- Tenant Subscription Plans
-- V4: Add tenant_plans table to manage free vs paid tiers

CREATE TABLE IF NOT EXISTS tenant_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    plan_name VARCHAR(30) NOT NULL DEFAULT 'FREE',  -- FREE, STARTER, PROFESSIONAL, ENTERPRISE
    max_users INT NOT NULL DEFAULT 3,
    max_custom_objects INT NOT NULL DEFAULT 2,
    max_workflows INT NOT NULL DEFAULT 3,
    max_dashboards INT NOT NULL DEFAULT 1,
    max_pipelines INT NOT NULL DEFAULT 1,
    max_roles INT NOT NULL DEFAULT 2,
    max_records_per_object INT NOT NULL DEFAULT 100,
    ai_config_enabled BOOLEAN NOT NULL DEFAULT true,
    ai_insights_enabled BOOLEAN NOT NULL DEFAULT false,
    email_tracking_enabled BOOLEAN NOT NULL DEFAULT false,
    api_access_enabled BOOLEAN NOT NULL DEFAULT false,
    integrations_enabled BOOLEAN NOT NULL DEFAULT false,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,  -- NULL = never expires (free tier)
    is_active BOOLEAN NOT NULL DEFAULT true,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_tenant_plans_tenant ON tenant_plans(tenant_id);
CREATE INDEX idx_tenant_plans_plan ON tenant_plans(plan_name);

-- Auto-create FREE plan for existing 'default' tenant
INSERT INTO tenant_plans (tenant_id, plan_name, max_users, max_custom_objects, max_workflows, max_dashboards, max_pipelines, max_roles, max_records_per_object, ai_config_enabled, ai_insights_enabled, email_tracking_enabled, api_access_enabled, integrations_enabled)
VALUES ('default', 'ENTERPRISE', 999, 999, 999, 999, 999, 999, 999999, true, true, true, true, true)
ON CONFLICT (tenant_id) DO NOTHING;
