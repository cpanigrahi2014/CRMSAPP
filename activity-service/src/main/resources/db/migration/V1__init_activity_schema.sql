-- Activity Service: Initial Schema
CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    due_date TIMESTAMP,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    assigned_to UUID,
    completed_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX idx_activities_tenant_id ON activities(tenant_id);
CREATE INDEX idx_activities_type ON activities(type);
CREATE INDEX idx_activities_status ON activities(status);
CREATE INDEX idx_activities_assigned_to ON activities(assigned_to);
CREATE INDEX idx_activities_related_entity_id ON activities(related_entity_id);
CREATE INDEX idx_activities_due_date ON activities(due_date);
CREATE INDEX idx_activities_created_at ON activities(created_at DESC);
