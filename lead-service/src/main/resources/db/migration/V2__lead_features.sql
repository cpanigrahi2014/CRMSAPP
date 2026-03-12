-- V2: Lead Management Features – notes, tags, attachments, activities, assignment rules, scoring rules, web forms

-- ── New columns on leads table ──────────────────────────────
ALTER TABLE leads ADD COLUMN IF NOT EXISTS campaign_id UUID;
ALTER TABLE leads ADD COLUMN IF NOT EXISTS territory VARCHAR(100);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS sla_due_date TIMESTAMP;
ALTER TABLE leads ADD COLUMN IF NOT EXISTS first_response_at TIMESTAMP;
ALTER TABLE leads ADD COLUMN IF NOT EXISTS account_id UUID;
ALTER TABLE leads ADD COLUMN IF NOT EXISTS contact_id UUID;

CREATE INDEX IF NOT EXISTS idx_leads_campaign_id ON leads(campaign_id);
CREATE INDEX IF NOT EXISTS idx_leads_territory ON leads(territory);

-- ── Lead Notes ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lead_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lead_notes_lead ON lead_notes(lead_id);

-- ── Lead Tags ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lead_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) DEFAULT '#1976d2',
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(name, tenant_id)
);

CREATE TABLE IF NOT EXISTS lead_tag_mappings (
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES lead_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (lead_id, tag_id)
);

-- ── Lead Attachments ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lead_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT DEFAULT 0,
    file_data BYTEA,
    tenant_id VARCHAR(100) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lead_attachments_lead ON lead_attachments(lead_id);

-- ── Lead Activities (timeline / history) ────────────────────
CREATE TABLE IF NOT EXISTS lead_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    activity_type VARCHAR(30) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    metadata JSONB,
    tenant_id VARCHAR(100) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lead_activities_lead ON lead_activities(lead_id);
CREATE INDEX IF NOT EXISTS idx_lead_activities_type ON lead_activities(activity_type);

-- ── Assignment Rules ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lead_assignment_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    criteria_field VARCHAR(50) NOT NULL,
    criteria_operator VARCHAR(20) NOT NULL DEFAULT 'EQUALS',
    criteria_value VARCHAR(255) NOT NULL,
    assign_to UUID NOT NULL,
    priority INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_assignment_rules_tenant ON lead_assignment_rules(tenant_id, active);

-- ── Scoring Rules ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lead_scoring_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    criteria_field VARCHAR(50) NOT NULL,
    criteria_operator VARCHAR(20) NOT NULL DEFAULT 'EQUALS',
    criteria_value VARCHAR(255) NOT NULL,
    score_delta INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN DEFAULT true,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_scoring_rules_tenant ON lead_scoring_rules(tenant_id, active);

-- ── Web Forms ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lead_web_forms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    fields JSONB NOT NULL DEFAULT '["firstName","lastName","email","phone","company"]',
    source VARCHAR(30) DEFAULT 'WEB',
    assign_to UUID,
    active BOOLEAN DEFAULT true,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_web_forms_tenant ON lead_web_forms(tenant_id, active);
