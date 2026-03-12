-- V3: Email Reply, Meeting Summary, Auto-Lead tables

-- AI-generated email replies
CREATE TABLE IF NOT EXISTS email_reply_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    original_from VARCHAR(500),
    original_subject VARCHAR(500),
    original_body TEXT,
    reply_subject VARCHAR(500),
    reply_body TEXT NOT NULL,
    tone VARCHAR(50) DEFAULT 'professional',
    suggestions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- AI meeting summaries
CREATE TABLE IF NOT EXISTS meeting_summary_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    meeting_title VARCHAR(500) NOT NULL,
    meeting_date TIMESTAMP,
    participants TEXT,
    transcript TEXT,
    summary TEXT NOT NULL,
    action_items TEXT,
    key_decisions TEXT,
    crm_updates TEXT,
    related_entity_type VARCHAR(50),
    related_entity_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Auto-created leads from emails/meetings
CREATE TABLE IF NOT EXISTS auto_lead_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_reference VARCHAR(500),
    lead_name VARCHAR(500) NOT NULL,
    email VARCHAR(500),
    company VARCHAR(500),
    title VARCHAR(300),
    phone VARCHAR(100),
    notes TEXT,
    confidence DECIMAL(5,4) NOT NULL DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_reply_tenant ON email_reply_records(tenant_id);
CREATE INDEX idx_meeting_summary_tenant ON meeting_summary_records(tenant_id);
CREATE INDEX idx_meeting_summary_date ON meeting_summary_records(meeting_date, tenant_id);
CREATE INDEX idx_auto_lead_tenant ON auto_lead_records(tenant_id);
CREATE INDEX idx_auto_lead_status ON auto_lead_records(status, tenant_id);
CREATE INDEX idx_auto_lead_source ON auto_lead_records(source_type, tenant_id);
