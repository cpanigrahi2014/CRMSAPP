-- V3: Pipeline features – stage history tracking & sales quota management

-- Stage history: tracks every stage transition for conversion analytics
CREATE TABLE IF NOT EXISTS stage_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id  UUID         NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    from_stage      VARCHAR(50),
    to_stage        VARCHAR(50)  NOT NULL,
    changed_by      VARCHAR(255),
    changed_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    time_in_stage   BIGINT,          -- seconds spent in previous stage
    tenant_id       VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_stage_history_opportunity ON stage_history(opportunity_id);
CREATE INDEX idx_stage_history_tenant      ON stage_history(tenant_id);
CREATE INDEX idx_stage_history_to_stage    ON stage_history(to_stage);
CREATE INDEX idx_stage_history_changed_at  ON stage_history(changed_at);

-- Sales quotas: per-user / per-period quota targets and actuals
CREATE TABLE IF NOT EXISTS sales_quotas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         VARCHAR(255) NOT NULL,
    period_type     VARCHAR(20)  NOT NULL,   -- MONTHLY, QUARTERLY, ANNUAL
    period_start    DATE         NOT NULL,
    period_end      DATE         NOT NULL,
    target_amount   NUMERIC(19,2) NOT NULL DEFAULT 0,
    target_deals    INTEGER       NOT NULL DEFAULT 0,
    actual_amount   NUMERIC(19,2) NOT NULL DEFAULT 0,
    actual_deals    INTEGER       NOT NULL DEFAULT 0,
    attainment_pct  NUMERIC(5,2)  NOT NULL DEFAULT 0,
    tenant_id       VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_sales_quota_user     ON sales_quotas(user_id);
CREATE INDEX idx_sales_quota_tenant   ON sales_quotas(tenant_id);
CREATE INDEX idx_sales_quota_period   ON sales_quotas(period_start, period_end);
