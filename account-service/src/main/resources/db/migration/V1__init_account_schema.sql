-- Account Service: Initial Schema
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    industry VARCHAR(100),
    website VARCHAR(255),
    phone VARCHAR(20),
    billing_address TEXT,
    shipping_address TEXT,
    annual_revenue NUMERIC(19, 2),
    number_of_employees INTEGER,
    parent_account_id UUID,
    description TEXT,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX idx_accounts_tenant_id ON accounts(tenant_id);
CREATE INDEX idx_accounts_name ON accounts(name);
CREATE INDEX idx_accounts_parent_account_id ON accounts(parent_account_id);
CREATE INDEX idx_accounts_industry ON accounts(industry);
CREATE INDEX idx_accounts_created_at ON accounts(created_at DESC);
