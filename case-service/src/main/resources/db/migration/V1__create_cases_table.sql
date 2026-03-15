-- V1__create_cases_table.sql
CREATE TABLE cases (
    id              UUID PRIMARY KEY,
    case_number     VARCHAR(20)  NOT NULL UNIQUE,
    subject         VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    origin          VARCHAR(20)  NOT NULL DEFAULT 'PORTAL',

    contact_name    VARCHAR(255),
    contact_email   VARCHAR(255),
    account_name    VARCHAR(255),
    contact_id      UUID,
    account_id      UUID,
    assigned_to     UUID,

    sla_due_date    TIMESTAMP,
    sla_met         BOOLEAN,
    escalated       BOOLEAN      NOT NULL DEFAULT FALSE,
    escalated_at    TIMESTAMP,
    resolved_at     TIMESTAMP,
    closed_at       TIMESTAMP,
    first_response_at TIMESTAMP,

    csat_score      INTEGER,
    csat_comment    TEXT,
    csat_sent       BOOLEAN      NOT NULL DEFAULT FALSE,

    resolution_notes TEXT,

    tenant_id       VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_case_tenant    ON cases (tenant_id);
CREATE INDEX idx_case_status    ON cases (status);
CREATE INDEX idx_case_priority  ON cases (priority);
CREATE INDEX idx_case_assigned  ON cases (assigned_to);
CREATE INDEX idx_case_number    ON cases (case_number);
CREATE INDEX idx_case_contact   ON cases (contact_id);
CREATE INDEX idx_case_account   ON cases (account_id);
CREATE INDEX idx_case_sla_due   ON cases (sla_due_date);
CREATE INDEX idx_case_escalated ON cases (escalated) WHERE escalated = TRUE;
CREATE INDEX idx_case_created   ON cases (created_at DESC);
