-- Opportunity Service: Initial Schema
CREATE TABLE IF NOT EXISTS opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    account_id UUID,
    contact_id UUID,
    amount NUMERIC(19, 2),
    stage VARCHAR(30) NOT NULL DEFAULT 'PROSPECTING',
    probability INTEGER,
    close_date DATE,
    description TEXT,
    assigned_to UUID,
    forecast_category VARCHAR(20) DEFAULT 'PIPELINE',
    lost_reason VARCHAR(500),
    won_date TIMESTAMP,
    lost_date TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX idx_opportunities_tenant_id ON opportunities(tenant_id);
CREATE INDEX idx_opportunities_stage ON opportunities(stage);
CREATE INDEX idx_opportunities_account_id ON opportunities(account_id);
CREATE INDEX idx_opportunities_assigned_to ON opportunities(assigned_to);
CREATE INDEX idx_opportunities_close_date ON opportunities(close_date);
CREATE INDEX idx_opportunities_created_at ON opportunities(created_at DESC);
