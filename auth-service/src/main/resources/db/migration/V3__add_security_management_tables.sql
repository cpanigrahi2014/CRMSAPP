-- Security Management: Permissions, Field Security, SSO, MFA, Audit Log

-- Permissions table (fine-grained permissions per role)
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    resource VARCHAR(100) NOT NULL,
    actions TEXT NOT NULL, -- comma-separated: create,read,update,delete
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, tenant_id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Field-level security rules
CREATE TABLE IF NOT EXISTS field_security_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    access_level VARCHAR(20) NOT NULL DEFAULT 'READ_WRITE',
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(entity_type, field_name, role_name, tenant_id)
);

-- SSO provider configurations
CREATE TABLE IF NOT EXISTS sso_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    client_id VARCHAR(500) NOT NULL,
    issuer_url VARCHAR(1000),
    metadata_url VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT false,
    auto_provision BOOLEAN NOT NULL DEFAULT false,
    default_role VARCHAR(50) DEFAULT 'USER',
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, tenant_id)
);

-- MFA configuration per user
CREATE TABLE IF NOT EXISTS mfa_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mfa_type VARCHAR(30) NOT NULL DEFAULT 'TOTP',
    enabled BOOLEAN NOT NULL DEFAULT false,
    secret_key VARCHAR(500),
    backup_codes TEXT,
    last_used_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, mfa_type)
);

-- Audit log for security events
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    user_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    details TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_permissions_tenant ON permissions(tenant_id);
CREATE INDEX idx_field_security_tenant ON field_security_rules(tenant_id);
CREATE INDEX idx_field_security_entity ON field_security_rules(entity_type, tenant_id);
CREATE INDEX idx_sso_providers_tenant ON sso_providers(tenant_id);
CREATE INDEX idx_mfa_configs_user ON mfa_configs(user_id);
CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action, tenant_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
