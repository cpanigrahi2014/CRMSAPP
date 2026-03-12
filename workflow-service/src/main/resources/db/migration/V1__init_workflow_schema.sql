-- Workflow Service: Initial Schema

CREATE TABLE IF NOT EXISTS workflow_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    entity_type VARCHAR(100) NOT NULL,
    trigger_event VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS workflow_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES workflow_rules(id) ON DELETE CASCADE,
    field_name VARCHAR(255) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    value VARCHAR(1000),
    logical_operator VARCHAR(10) DEFAULT 'AND'
);

CREATE TABLE IF NOT EXISTS workflow_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES workflow_rules(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    target_field VARCHAR(255),
    target_value TEXT,
    action_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS workflow_execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL,
    trigger_entity_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    execution_details TEXT,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

-- Indexes for workflow_rules
CREATE INDEX idx_workflow_rules_tenant_id ON workflow_rules(tenant_id);
CREATE INDEX idx_workflow_rules_entity_type ON workflow_rules(entity_type);
CREATE INDEX idx_workflow_rules_trigger_event ON workflow_rules(trigger_event);
CREATE INDEX idx_workflow_rules_active ON workflow_rules(active);
CREATE INDEX idx_workflow_rules_tenant_entity_event ON workflow_rules(tenant_id, entity_type, trigger_event) WHERE active = true AND deleted = false;

-- Indexes for workflow_conditions
CREATE INDEX idx_workflow_conditions_rule_id ON workflow_conditions(rule_id);

-- Indexes for workflow_actions
CREATE INDEX idx_workflow_actions_rule_id ON workflow_actions(rule_id);

-- Indexes for workflow_execution_logs
CREATE INDEX idx_workflow_exec_logs_tenant_id ON workflow_execution_logs(tenant_id);
CREATE INDEX idx_workflow_exec_logs_rule_id ON workflow_execution_logs(rule_id);
CREATE INDEX idx_workflow_exec_logs_status ON workflow_execution_logs(status);
CREATE INDEX idx_workflow_exec_logs_executed_at ON workflow_execution_logs(executed_at DESC);
