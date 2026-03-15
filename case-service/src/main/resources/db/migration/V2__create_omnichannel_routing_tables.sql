-- V2__create_omnichannel_routing_tables.sql
-- Omnichannel Routing: queues, agent presence, skills, rules, work items

-- ═══ Routing Queues ═══
CREATE TABLE routing_queues (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(500),
    channel             VARCHAR(20)  NOT NULL DEFAULT 'CASE',
    routing_model       VARCHAR(30)  NOT NULL DEFAULT 'LEAST_ACTIVE',
    priority_weight     INTEGER      NOT NULL DEFAULT 1,
    max_wait_seconds    INTEGER      DEFAULT 300,
    overflow_queue_id   UUID,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,

    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_rq_tenant  ON routing_queues (tenant_id);
CREATE INDEX idx_rq_name    ON routing_queues (name);
CREATE INDEX idx_rq_channel ON routing_queues (channel);
CREATE INDEX idx_rq_active  ON routing_queues (active) WHERE active = TRUE;

-- ═══ Agent Presence ═══
CREATE TABLE agent_presence (
    id                  UUID PRIMARY KEY,
    user_id             UUID         NOT NULL,
    agent_name          VARCHAR(255),
    agent_email         VARCHAR(255),
    status              VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    queue_id            UUID,
    capacity            INTEGER      NOT NULL DEFAULT 5,
    active_work_count   INTEGER      NOT NULL DEFAULT 0,
    last_routed_at      TIMESTAMP,
    status_changed_at   TIMESTAMP,
    auto_accept         BOOLEAN      NOT NULL DEFAULT FALSE,

    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ap_tenant  ON agent_presence (tenant_id);
CREATE INDEX idx_ap_user    ON agent_presence (user_id);
CREATE INDEX idx_ap_status  ON agent_presence (status);
CREATE INDEX idx_ap_queue   ON agent_presence (queue_id);
CREATE INDEX idx_ap_avail   ON agent_presence (tenant_id, queue_id, status, active_work_count);

-- ═══ Agent Skills ═══
CREATE TABLE agent_skills (
    id                  UUID PRIMARY KEY,
    user_id             UUID         NOT NULL,
    skill_name          VARCHAR(100) NOT NULL,
    proficiency         INTEGER      NOT NULL DEFAULT 3,
    category            VARCHAR(50),

    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_as_tenant  ON agent_skills (tenant_id);
CREATE INDEX idx_as_user    ON agent_skills (user_id);
CREATE INDEX idx_as_skill   ON agent_skills (skill_name);

-- ═══ Routing Rules ═══
CREATE TABLE routing_rules (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(500),
    queue_id            UUID         NOT NULL,
    match_field         VARCHAR(30)  NOT NULL,
    match_operator      VARCHAR(20)  NOT NULL DEFAULT 'EQUALS',
    match_value         VARCHAR(500) NOT NULL,
    required_skill      VARCHAR(100),
    min_proficiency     INTEGER      DEFAULT 1,
    rule_priority       INTEGER      NOT NULL DEFAULT 10,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,

    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_rr_tenant   ON routing_rules (tenant_id);
CREATE INDEX idx_rr_queue    ON routing_rules (queue_id);
CREATE INDEX idx_rr_priority ON routing_rules (rule_priority);
CREATE INDEX idx_rr_active   ON routing_rules (active) WHERE active = TRUE;

-- ═══ Work Items ═══
CREATE TABLE work_items (
    id                  UUID PRIMARY KEY,
    entity_type         VARCHAR(30)  NOT NULL,
    entity_id           UUID         NOT NULL,
    queue_id            UUID,
    assigned_agent_id   UUID,
    status              VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    priority            VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    channel             VARCHAR(20),
    subject             VARCHAR(500),
    queued_at           TIMESTAMP,
    assigned_at         TIMESTAMP,
    accepted_at         TIMESTAMP,
    completed_at        TIMESTAMP,
    declined_count      INTEGER      NOT NULL DEFAULT 0,
    wait_time_seconds   BIGINT,
    handle_time_seconds BIGINT,

    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_wi_tenant  ON work_items (tenant_id);
CREATE INDEX idx_wi_queue   ON work_items (queue_id);
CREATE INDEX idx_wi_agent   ON work_items (assigned_agent_id);
CREATE INDEX idx_wi_status  ON work_items (status);
CREATE INDEX idx_wi_entity  ON work_items (entity_type, entity_id);
CREATE INDEX idx_wi_queued  ON work_items (status, priority DESC, created_at ASC)
    WHERE status = 'QUEUED';
