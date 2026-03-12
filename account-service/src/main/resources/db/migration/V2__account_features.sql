-- Account Service: V2 Features Migration

-- Add new columns to accounts table
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'PROSPECT';
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS owner_id VARCHAR(255);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS territory VARCHAR(100);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS lifecycle_stage VARCHAR(50) DEFAULT 'NEW';
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS health_score INTEGER DEFAULT 50;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS segment VARCHAR(100);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS engagement_score INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(type);
CREATE INDEX IF NOT EXISTS idx_accounts_owner ON accounts(owner_id);
CREATE INDEX IF NOT EXISTS idx_accounts_territory ON accounts(territory);
CREATE INDEX IF NOT EXISTS idx_accounts_lifecycle ON accounts(lifecycle_stage);

-- Account Notes
CREATE TABLE IF NOT EXISTS account_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    content TEXT NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_acct_notes_account ON account_notes(account_id);
CREATE INDEX idx_acct_notes_tenant ON account_notes(tenant_id);

-- Account Tags
CREATE TABLE IF NOT EXISTS account_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20) DEFAULT '#1976d2',
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(name, tenant_id)
);
CREATE INDEX idx_acct_tags_tenant ON account_tags(tenant_id);

-- Account Tag Mappings
CREATE TABLE IF NOT EXISTS account_tag_mappings (
    account_id UUID NOT NULL REFERENCES accounts(id),
    tag_id UUID NOT NULL REFERENCES account_tags(id),
    PRIMARY KEY (account_id, tag_id)
);

-- Account Attachments
CREATE TABLE IF NOT EXISTS account_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_acct_attach_account ON account_attachments(account_id);
CREATE INDEX idx_acct_attach_tenant ON account_attachments(tenant_id);

-- Account Activities
CREATE TABLE IF NOT EXISTS account_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    performed_by VARCHAR(255),
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_acct_activity_account ON account_activities(account_id);
CREATE INDEX idx_acct_activity_tenant ON account_activities(tenant_id);
CREATE INDEX idx_acct_activity_type ON account_activities(type);
