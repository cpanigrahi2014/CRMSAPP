-- AI Service: Initial Schema
CREATE TABLE IF NOT EXISTS ai_request_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    input_data TEXT,
    output_data TEXT,
    model VARCHAR(100),
    tokens_used INTEGER,
    latency_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_request_log_tenant_id ON ai_request_log(tenant_id);
CREATE INDEX idx_ai_request_log_request_type ON ai_request_log(request_type);
CREATE INDEX idx_ai_request_log_created_at ON ai_request_log(created_at DESC);
CREATE INDEX idx_ai_request_log_model ON ai_request_log(model);
