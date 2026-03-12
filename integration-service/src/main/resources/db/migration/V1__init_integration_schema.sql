-- Integration Service: Initial Schema

-- REST API Endpoints
CREATE TABLE IF NOT EXISTS api_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    description TEXT,
    auth_required BOOLEAN NOT NULL DEFAULT true,
    rate_limit INTEGER DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT true,
    version VARCHAR(20) DEFAULT 'v1',
    total_calls BIGINT DEFAULT 0,
    last_called_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Webhook Configurations
CREATE TABLE IF NOT EXISTS webhook_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    events TEXT NOT NULL, -- JSON array
    active BOOLEAN NOT NULL DEFAULT true,
    secret_key VARCHAR(500),
    retry_count INTEGER DEFAULT 3,
    retry_delay_ms INTEGER DEFAULT 5000,
    success_count BIGINT DEFAULT 0,
    failure_count BIGINT DEFAULT 0,
    last_triggered_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Third-Party Integrations
CREATE TABLE IF NOT EXISTS third_party_integrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    description TEXT,
    auth_type VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT false,
    config TEXT, -- JSON config
    last_sync_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Data Sync Configurations
CREATE TABLE IF NOT EXISTS data_syncs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    integration_id UUID NOT NULL REFERENCES third_party_integrations(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    direction VARCHAR(20) NOT NULL, -- INBOUND, OUTBOUND, BIDIRECTIONAL
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    schedule VARCHAR(100),
    last_run_at TIMESTAMP,
    last_run_duration BIGINT,
    records_synced BIGINT DEFAULT 0,
    records_failed BIGINT DEFAULT 0,
    field_mapping TEXT, -- JSON
    enabled BOOLEAN NOT NULL DEFAULT true,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- External Connectors
CREATE TABLE IF NOT EXISTS external_connectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- DATABASE, REST_API, FILE, MESSAGE_QUEUE
    host VARCHAR(500),
    port INTEGER,
    database_name VARCHAR(255),
    base_url VARCHAR(1000),
    connection_string TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    enabled BOOLEAN NOT NULL DEFAULT false,
    last_test_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- API Authentication Configurations
CREATE TABLE IF NOT EXISTS api_auth_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    auth_type VARCHAR(50) NOT NULL, -- API_KEY, OAUTH2, BEARER, BASIC
    client_id VARCHAR(500),
    token_url VARCHAR(1000),
    scopes TEXT, -- JSON array
    active BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Integration Health Records
CREATE TABLE IF NOT EXISTS integration_health (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    integration_id UUID NOT NULL REFERENCES third_party_integrations(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    uptime NUMERIC(5,2),
    avg_response_ms INTEGER,
    success_rate NUMERIC(5,2),
    total_requests BIGINT DEFAULT 0,
    last_checked_at TIMESTAMP,
    alerts_count INTEGER DEFAULT 0,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Integration Error Logs
CREATE TABLE IF NOT EXISTS integration_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    integration_id UUID NOT NULL REFERENCES third_party_integrations(id) ON DELETE CASCADE,
    level VARCHAR(20) NOT NULL, -- CRITICAL, ERROR, WARN, INFO
    message TEXT NOT NULL,
    endpoint VARCHAR(500),
    http_status INTEGER,
    request_payload TEXT,
    resolved_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_api_endpoints_tenant ON api_endpoints(tenant_id);
CREATE INDEX idx_webhook_configs_tenant ON webhook_configs(tenant_id);
CREATE INDEX idx_third_party_integrations_tenant ON third_party_integrations(tenant_id);
CREATE INDEX idx_third_party_integrations_status ON third_party_integrations(status);
CREATE INDEX idx_data_syncs_tenant ON data_syncs(tenant_id);
CREATE INDEX idx_data_syncs_integration ON data_syncs(integration_id);
CREATE INDEX idx_external_connectors_tenant ON external_connectors(tenant_id);
CREATE INDEX idx_api_auth_configs_tenant ON api_auth_configs(tenant_id);
CREATE INDEX idx_integration_health_integration ON integration_health(integration_id);
CREATE INDEX idx_integration_health_tenant ON integration_health(tenant_id);
CREATE INDEX idx_integration_errors_integration ON integration_errors(integration_id);
CREATE INDEX idx_integration_errors_tenant ON integration_errors(tenant_id);
CREATE INDEX idx_integration_errors_level ON integration_errors(level);
CREATE INDEX idx_integration_errors_created ON integration_errors(created_at DESC);
