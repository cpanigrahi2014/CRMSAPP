-- Developer & Integration Platform: webhook delivery, API keys, marketplace, widgets, low-code apps

-- Webhook Delivery Logs
CREATE TABLE IF NOT EXISTS webhook_delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id UUID NOT NULL REFERENCES webhook_configs(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    response_status INTEGER,
    response_body TEXT,
    attempt INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, RETRYING
    error_message TEXT,
    delivered_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Developer API Keys
CREATE TABLE IF NOT EXISTS developer_api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(10) NOT NULL,
    key_hash VARCHAR(500) NOT NULL,
    scopes TEXT, -- JSON array of allowed scopes
    rate_limit INTEGER DEFAULT 1000,
    calls_today BIGINT DEFAULT 0,
    total_calls BIGINT DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    created_by VARCHAR(255),
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Marketplace Plugins
CREATE TABLE IF NOT EXISTS marketplace_plugins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    long_description TEXT,
    category VARCHAR(100) NOT NULL,
    author VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
    icon_url VARCHAR(1000),
    screenshots TEXT, -- JSON array of URLs
    download_url VARCHAR(1000),
    documentation_url VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED', -- DRAFT, PUBLISHED, DEPRECATED, REMOVED
    pricing VARCHAR(20) NOT NULL DEFAULT 'FREE', -- FREE, PAID, FREEMIUM
    price_amount NUMERIC(10,2),
    install_count BIGINT DEFAULT 0,
    rating NUMERIC(3,2) DEFAULT 0,
    rating_count INTEGER DEFAULT 0,
    required_scopes TEXT, -- JSON array
    config_schema TEXT, -- JSON schema for plugin config
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Plugin Installations (per tenant)
CREATE TABLE IF NOT EXISTS plugin_installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id UUID NOT NULL REFERENCES marketplace_plugins(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED, UNINSTALLED
    config TEXT, -- JSON plugin config overrides
    installed_by VARCHAR(255),
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(plugin_id, tenant_id)
);

-- Embeddable Widgets
CREATE TABLE IF NOT EXISTS embeddable_widgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    widget_type VARCHAR(50) NOT NULL, -- FORM, TABLE, CHART, METRIC, TIMELINE, CUSTOM
    description TEXT,
    config TEXT NOT NULL, -- JSON config (data source, filters, style)
    embed_token VARCHAR(500) NOT NULL UNIQUE,
    allowed_domains TEXT, -- JSON array of allowed domains
    active BOOLEAN NOT NULL DEFAULT true,
    view_count BIGINT DEFAULT 0,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Low-Code Custom Apps
CREATE TABLE IF NOT EXISTS custom_apps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    app_type VARCHAR(50) NOT NULL DEFAULT 'FORM', -- FORM, DASHBOARD, PAGE, WORKFLOW
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    layout TEXT NOT NULL, -- JSON layout definition (components, positions, data bindings)
    data_source TEXT, -- JSON data source config (entities, filters)
    style TEXT, -- JSON custom styles
    published_version VARCHAR(50) DEFAULT '0',
    created_by VARCHAR(255),
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_webhook_delivery_logs_webhook ON webhook_delivery_logs(webhook_id);
CREATE INDEX idx_webhook_delivery_logs_tenant ON webhook_delivery_logs(tenant_id);
CREATE INDEX idx_webhook_delivery_logs_status ON webhook_delivery_logs(status);
CREATE INDEX idx_developer_api_keys_tenant ON developer_api_keys(tenant_id);
CREATE INDEX idx_developer_api_keys_prefix ON developer_api_keys(key_prefix);
CREATE INDEX idx_marketplace_plugins_category ON marketplace_plugins(category);
CREATE INDEX idx_marketplace_plugins_slug ON marketplace_plugins(slug);
CREATE INDEX idx_plugin_installations_tenant ON plugin_installations(tenant_id);
CREATE INDEX idx_plugin_installations_plugin ON plugin_installations(plugin_id);
CREATE INDEX idx_embeddable_widgets_tenant ON embeddable_widgets(tenant_id);
CREATE INDEX idx_embeddable_widgets_token ON embeddable_widgets(embed_token);
CREATE INDEX idx_custom_apps_tenant ON custom_apps(tenant_id);
CREATE INDEX idx_custom_apps_slug ON custom_apps(slug);
