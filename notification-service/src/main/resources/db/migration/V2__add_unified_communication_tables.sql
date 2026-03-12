-- Notification Service V2: Add unified communication tables (WhatsApp, SMS, Call)

-- ── SMS Messages ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    direction VARCHAR(10) NOT NULL DEFAULT 'OUTBOUND',
    from_number VARCHAR(20),
    to_number VARCHAR(20) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_id VARCHAR(255),
    error_message TEXT,
    related_entity_type VARCHAR(100),
    related_entity_id UUID,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX idx_sms_tenant ON sms_messages(tenant_id);
CREATE INDEX idx_sms_to_number ON sms_messages(to_number);
CREATE INDEX idx_sms_status ON sms_messages(status);
CREATE INDEX idx_sms_direction ON sms_messages(direction);
CREATE INDEX idx_sms_created_at ON sms_messages(tenant_id, created_at DESC);

-- ── WhatsApp Messages ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    direction VARCHAR(10) NOT NULL DEFAULT 'OUTBOUND',
    from_number VARCHAR(20),
    to_number VARCHAR(20) NOT NULL,
    body TEXT,
    media_url VARCHAR(1000),
    media_type VARCHAR(50),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_id VARCHAR(255),
    error_message TEXT,
    related_entity_type VARCHAR(100),
    related_entity_id UUID,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX idx_wa_tenant ON whatsapp_messages(tenant_id);
CREATE INDEX idx_wa_to_number ON whatsapp_messages(to_number);
CREATE INDEX idx_wa_status ON whatsapp_messages(status);
CREATE INDEX idx_wa_direction ON whatsapp_messages(direction);
CREATE INDEX idx_wa_created_at ON whatsapp_messages(tenant_id, created_at DESC);

-- ── Call Records ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS call_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    direction VARCHAR(10) NOT NULL DEFAULT 'OUTBOUND',
    from_number VARCHAR(20),
    to_number VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    duration_seconds INTEGER DEFAULT 0,
    recording_url VARCHAR(1000),
    recording_duration_seconds INTEGER DEFAULT 0,
    voicemail_url VARCHAR(1000),
    call_outcome VARCHAR(50),
    notes TEXT,
    external_id VARCHAR(255),
    error_message TEXT,
    related_entity_type VARCHAR(100),
    related_entity_id UUID,
    started_at TIMESTAMP,
    answered_at TIMESTAMP,
    ended_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX idx_call_tenant ON call_records(tenant_id);
CREATE INDEX idx_call_to_number ON call_records(to_number);
CREATE INDEX idx_call_status ON call_records(status);
CREATE INDEX idx_call_direction ON call_records(direction);
CREATE INDEX idx_call_created_at ON call_records(tenant_id, created_at DESC);

-- ── Unified Communication View ───────────────────────────────
CREATE TABLE IF NOT EXISTS unified_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    sender VARCHAR(255),
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    body TEXT,
    status VARCHAR(20) NOT NULL,
    source_id UUID NOT NULL,
    related_entity_type VARCHAR(100),
    related_entity_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_unified_tenant ON unified_messages(tenant_id);
CREATE INDEX idx_unified_channel ON unified_messages(channel);
CREATE INDEX idx_unified_recipient ON unified_messages(recipient);
CREATE INDEX idx_unified_entity ON unified_messages(related_entity_type, related_entity_id);
CREATE INDEX idx_unified_created_at ON unified_messages(tenant_id, created_at DESC);
