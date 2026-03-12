-- V2: Opportunity Features – products, competitors, activities, collaborators, reminders, alerts
-- Also add new columns to opportunities table

-- New columns on opportunities
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS next_step VARCHAR(500);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS lead_source VARCHAR(100);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS campaign_id UUID;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS predicted_close_date DATE;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS confidence_score INTEGER;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS owner_id UUID;

-- Opportunity products / line items
CREATE TABLE IF NOT EXISTS opportunity_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    product_code VARCHAR(100),
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    discount NUMERIC(5, 2) DEFAULT 0,
    total_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    description TEXT,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_opp_products_opportunity ON opportunity_products(opportunity_id);
CREATE INDEX idx_opp_products_tenant ON opportunity_products(tenant_id);

-- Opportunity competitors
CREATE TABLE IF NOT EXISTS opportunity_competitors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    competitor_name VARCHAR(255) NOT NULL,
    strengths TEXT,
    weaknesses TEXT,
    strategy TEXT,
    threat_level VARCHAR(20) DEFAULT 'MEDIUM',
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_opp_competitors_opportunity ON opportunity_competitors(opportunity_id);
CREATE INDEX idx_opp_competitors_tenant ON opportunity_competitors(tenant_id);

-- Opportunity activities / timeline
CREATE TABLE IF NOT EXISTS opportunity_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    description TEXT,
    metadata TEXT,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255)
);
CREATE INDEX idx_opp_activities_opportunity ON opportunity_activities(opportunity_id);
CREATE INDEX idx_opp_activities_tenant ON opportunity_activities(tenant_id);

-- Opportunity collaborators
CREATE TABLE IF NOT EXISTS opportunity_collaborators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(opportunity_id, user_id)
);
CREATE INDEX idx_opp_collaborators_opportunity ON opportunity_collaborators(opportunity_id);
CREATE INDEX idx_opp_collaborators_user ON opportunity_collaborators(user_id);

-- Opportunity notes (collaboration)
CREATE TABLE IF NOT EXISTS opportunity_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_pinned BOOLEAN DEFAULT false,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);
CREATE INDEX idx_opp_notes_opportunity ON opportunity_notes(opportunity_id);

-- Opportunity reminders
CREATE TABLE IF NOT EXISTS opportunity_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    reminder_type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    remind_at TIMESTAMP NOT NULL,
    is_completed BOOLEAN DEFAULT false,
    completed_at TIMESTAMP,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255)
);
CREATE INDEX idx_opp_reminders_opportunity ON opportunity_reminders(opportunity_id);
CREATE INDEX idx_opp_reminders_remind_at ON opportunity_reminders(remind_at);
CREATE INDEX idx_opp_reminders_tenant ON opportunity_reminders(tenant_id);

-- Additional indexes on opportunities
CREATE INDEX IF NOT EXISTS idx_opportunities_lead_source ON opportunities(lead_source);
CREATE INDEX IF NOT EXISTS idx_opportunities_currency ON opportunities(currency);
