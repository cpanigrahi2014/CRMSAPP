-- V4: Lead Score Records table

CREATE TABLE IF NOT EXISTS lead_score_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    lead_id VARCHAR(255) NOT NULL,
    lead_name VARCHAR(500),
    email VARCHAR(500),
    company VARCHAR(500),
    current_score INT DEFAULT 0,
    predicted_score INT DEFAULT 0,
    trend VARCHAR(20) DEFAULT 'STABLE',
    conversion_probability DECIMAL(5,4) DEFAULT 0,
    top_factors TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lead_score_tenant ON lead_score_records(tenant_id);
CREATE INDEX IF NOT EXISTS idx_lead_score_lead_id ON lead_score_records(lead_id);
