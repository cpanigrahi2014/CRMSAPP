-- V4: Real-Time Collaboration features
-- Deal chat, mentions, approvals, and record comments

-- ── Deal Chat Messages ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS deal_chat_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id      UUID         NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    sender_id           VARCHAR(255) NOT NULL,
    sender_name         VARCHAR(255),
    message             TEXT         NOT NULL,
    message_type        VARCHAR(50)  NOT NULL DEFAULT 'TEXT',
    parent_message_id   UUID         REFERENCES deal_chat_messages(id),
    is_edited           BOOLEAN      NOT NULL DEFAULT FALSE,
    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_deal_chat_opportunity ON deal_chat_messages(opportunity_id);
CREATE INDEX idx_deal_chat_tenant      ON deal_chat_messages(tenant_id);
CREATE INDEX idx_deal_chat_sender      ON deal_chat_messages(sender_id);
CREATE INDEX idx_deal_chat_created     ON deal_chat_messages(created_at);
CREATE INDEX idx_deal_chat_parent      ON deal_chat_messages(parent_message_id);

-- ── Mentions ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mentions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_type         VARCHAR(50)  NOT NULL,
    record_id           UUID         NOT NULL,
    source_type         VARCHAR(50)  NOT NULL,
    source_id           UUID         NOT NULL,
    mentioned_user_id   VARCHAR(255) NOT NULL,
    mentioned_user_name VARCHAR(255),
    mentioned_by_id     VARCHAR(255) NOT NULL,
    mentioned_by_name   VARCHAR(255),
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mention_user      ON mentions(mentioned_user_id);
CREATE INDEX idx_mention_record    ON mentions(record_type, record_id);
CREATE INDEX idx_mention_source    ON mentions(source_type, source_id);
CREATE INDEX idx_mention_tenant    ON mentions(tenant_id);
CREATE INDEX idx_mention_unread    ON mentions(mentioned_user_id, is_read) WHERE is_read = FALSE;

-- ── Deal Approvals ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS deal_approvals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id      UUID         NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    requested_by_id     VARCHAR(255) NOT NULL,
    requested_by_name   VARCHAR(255),
    approver_id         VARCHAR(255) NOT NULL,
    approver_name       VARCHAR(255),
    approval_type       VARCHAR(50)  NOT NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    title               VARCHAR(500),
    description         TEXT,
    current_value       VARCHAR(255),
    requested_value     VARCHAR(255),
    approver_comment    TEXT,
    priority            VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    due_date            TIMESTAMP,
    decided_at          TIMESTAMP,
    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_deal_approval_opp      ON deal_approvals(opportunity_id);
CREATE INDEX idx_deal_approval_approver ON deal_approvals(approver_id);
CREATE INDEX idx_deal_approval_status   ON deal_approvals(status);
CREATE INDEX idx_deal_approval_tenant   ON deal_approvals(tenant_id);
CREATE INDEX idx_deal_approval_pending  ON deal_approvals(approver_id, status) WHERE status = 'PENDING';

-- ── Record Comments ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS record_comments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_type         VARCHAR(50)  NOT NULL,
    record_id           UUID         NOT NULL,
    author_id           VARCHAR(255) NOT NULL,
    author_name         VARCHAR(255),
    content             TEXT         NOT NULL,
    parent_comment_id   UUID         REFERENCES record_comments(id),
    is_internal         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_edited           BOOLEAN      NOT NULL DEFAULT FALSE,
    is_pinned           BOOLEAN      NOT NULL DEFAULT FALSE,
    tenant_id           VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_record_comment_record  ON record_comments(record_type, record_id);
CREATE INDEX idx_record_comment_author  ON record_comments(author_id);
CREATE INDEX idx_record_comment_parent  ON record_comments(parent_comment_id);
CREATE INDEX idx_record_comment_tenant  ON record_comments(tenant_id);
CREATE INDEX idx_record_comment_pinned  ON record_comments(record_type, record_id, is_pinned) WHERE is_pinned = TRUE;
