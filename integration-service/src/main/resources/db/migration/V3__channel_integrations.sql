-- Channel Integration: WhatsApp, Email, Social Media webhook tracking

CREATE TABLE IF NOT EXISTS channel_webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel VARCHAR(50) NOT NULL,       -- WHATSAPP, EMAIL, INSTAGRAM, FACEBOOK, LINKEDIN, TWITTER
    event_type VARCHAR(100) NOT NULL,   -- WHATSAPP_LEAD_CREATED, EMAIL_CASE_CREATED, SOCIAL_LEAD_CREATED
    source_identifier VARCHAR(500),     -- phone number, email, social username
    lead_id UUID,
    case_id UUID,
    opportunity_id UUID,
    activity_id UUID,
    payload TEXT,                        -- JSON payload
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS', -- SUCCESS, FAILED, PARTIAL
    error_message TEXT,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_channel_log_tenant ON channel_webhook_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_channel_log_channel ON channel_webhook_logs(channel);
CREATE INDEX IF NOT EXISTS idx_channel_log_created ON channel_webhook_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_channel_log_lead ON channel_webhook_logs(lead_id);
CREATE INDEX IF NOT EXISTS idx_channel_log_case ON channel_webhook_logs(case_id);
