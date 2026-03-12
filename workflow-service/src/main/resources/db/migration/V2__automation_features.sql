-- ================================================================
-- V2: Smart Automation Features
-- Adds: workflow templates, visual builder metadata,
--        AI suggestions, proposal templates, contracts
-- ================================================================

-- ── Workflow visual builder metadata ────────────────────────────
ALTER TABLE workflow_rules ADD COLUMN IF NOT EXISTS
    canvas_layout TEXT;  -- JSON: node positions for visual builder

ALTER TABLE workflow_rules ADD COLUMN IF NOT EXISTS
    template_id UUID;    -- if created from a template

-- ── Workflow Templates (pre-built automations) ─────────────────
CREATE TABLE IF NOT EXISTS workflow_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,        -- LEAD_MANAGEMENT, DEAL_PIPELINE, FOLLOW_UP, NOTIFICATION, CUSTOM
    entity_type VARCHAR(50) NOT NULL,
    trigger_event VARCHAR(50) NOT NULL,
    conditions_json TEXT,                   -- JSON array of condition configs
    actions_json TEXT,                      -- JSON array of action configs
    canvas_layout TEXT,                     -- Default visual layout
    popularity INTEGER DEFAULT 0,
    is_system BOOLEAN DEFAULT FALSE,       -- system-provided vs user-created
    tenant_id VARCHAR(100),                -- NULL = global templates
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_templates_category ON workflow_templates(category);
CREATE INDEX IF NOT EXISTS idx_workflow_templates_tenant ON workflow_templates(tenant_id);

-- ── AI Workflow Suggestions ────────────────────────────────────
CREATE TABLE IF NOT EXISTS workflow_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    suggestion_type VARCHAR(50) NOT NULL,  -- PATTERN_DETECTED, BEST_PRACTICE, OPTIMIZATION
    entity_type VARCHAR(50) NOT NULL,
    trigger_event VARCHAR(50),
    conditions_json TEXT,
    actions_json TEXT,
    canvas_layout TEXT,
    confidence DOUBLE PRECISION DEFAULT 0.5,
    reason TEXT,                            -- why suggested
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, ACCEPTED, DISMISSED
    accepted_rule_id UUID,                 -- rule created from suggestion
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_suggestions_tenant ON workflow_suggestions(tenant_id, status);

-- ── Proposal Templates ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS proposal_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    content_template TEXT NOT NULL,         -- Markdown/HTML with {{placeholders}}
    category VARCHAR(100),                  -- STANDARD, ENTERPRISE, CUSTOM
    is_default BOOLEAN DEFAULT FALSE,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_proposal_templates_tenant ON proposal_templates(tenant_id);

-- ── Proposals ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL,
    template_id UUID,
    title VARCHAR(255) NOT NULL,
    content TEXT,                           -- Generated content
    status VARCHAR(30) DEFAULT 'DRAFT',    -- DRAFT, SENT, VIEWED, ACCEPTED, REJECTED, EXPIRED
    amount DECIMAL(19,2),
    valid_until DATE,
    sent_at TIMESTAMP,
    viewed_at TIMESTAMP,
    responded_at TIMESTAMP,
    recipient_email VARCHAR(255),
    recipient_name VARCHAR(255),
    notes TEXT,
    version INTEGER DEFAULT 1,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_proposals_opportunity ON proposals(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_proposals_tenant ON proposals(tenant_id, status) WHERE deleted = FALSE;

-- ── Proposal Line Items ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS proposal_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity INTEGER DEFAULT 1,
    unit_price DECIMAL(19,2) NOT NULL,
    discount DECIMAL(5,2) DEFAULT 0,
    total_price DECIMAL(19,2) NOT NULL,
    sort_order INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proposal_items_proposal ON proposal_line_items(proposal_id);

-- ── Contracts ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL,
    proposal_id UUID,                       -- optional: linked to proposal
    title VARCHAR(255) NOT NULL,
    content TEXT,                           -- Contract body
    status VARCHAR(30) DEFAULT 'DRAFT',    -- DRAFT, SENT, VIEWED, SIGNED, EXECUTED, EXPIRED, CANCELLED
    amount DECIMAL(19,2),
    start_date DATE,
    end_date DATE,
    sent_at TIMESTAMP,
    viewed_at TIMESTAMP,
    signed_at TIMESTAMP,
    executed_at TIMESTAMP,
    signer_name VARCHAR(255),
    signer_email VARCHAR(255),
    signer_ip VARCHAR(45),
    signature_data TEXT,                   -- base64 signature image
    notes TEXT,
    version INTEGER DEFAULT 1,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_contracts_opportunity ON contracts(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_contracts_tenant ON contracts(tenant_id, status) WHERE deleted = FALSE;
