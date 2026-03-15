-- Campaigns table
CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_date DATE,
    end_date DATE,
    budget DECIMAL(15,2) DEFAULT 0,
    actual_cost DECIMAL(15,2) DEFAULT 0,
    expected_revenue DECIMAL(15,2) DEFAULT 0,
    won_revenue DECIMAL(15,2) DEFAULT 0,
    description TEXT,
    number_sent INTEGER DEFAULT 0,
    leads_generated INTEGER DEFAULT 0,
    conversions INTEGER DEFAULT 0,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_campaign_tenant ON campaigns(tenant_id);
CREATE INDEX idx_campaign_status ON campaigns(status);
CREATE INDEX idx_campaign_type ON campaigns(type);
CREATE INDEX idx_campaign_created ON campaigns(created_at DESC);

-- Campaign members table
CREATE TABLE campaign_members (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    lead_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP,
    converted_at TIMESTAMP,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_cm_campaign ON campaign_members(campaign_id);
CREATE INDEX idx_cm_lead ON campaign_members(lead_id);
CREATE INDEX idx_cm_tenant ON campaign_members(tenant_id);
CREATE INDEX idx_cm_status ON campaign_members(status);
CREATE UNIQUE INDEX idx_cm_campaign_lead ON campaign_members(campaign_id, lead_id) WHERE deleted = false;
