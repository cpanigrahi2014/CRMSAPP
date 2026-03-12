-- V3: Activity stream for real-time updates

CREATE TABLE IF NOT EXISTS activity_stream (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       UUID,
    entity_name     VARCHAR(255),
    description     TEXT,
    performed_by    VARCHAR(255),
    performed_by_name VARCHAR(255),
    metadata        TEXT,
    tenant_id       VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_stream_tenant    ON activity_stream(tenant_id);
CREATE INDEX idx_activity_stream_entity    ON activity_stream(entity_type, entity_id);
CREATE INDEX idx_activity_stream_type      ON activity_stream(event_type);
CREATE INDEX idx_activity_stream_created   ON activity_stream(created_at);
CREATE INDEX idx_activity_stream_user      ON activity_stream(performed_by);
