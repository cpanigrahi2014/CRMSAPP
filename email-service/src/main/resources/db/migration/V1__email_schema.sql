-- ============================================================
-- V1 – Email service schema
-- Tables: email_accounts, email_templates, email_messages,
--         email_tracking_events, email_schedules
-- ============================================================

-- 1. Email accounts (Gmail / Outlook / SMTP connections)
CREATE TABLE email_accounts (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    provider        VARCHAR(20)  NOT NULL,          -- GMAIL, OUTLOOK, SMTP
    email           VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255),
    access_token    TEXT,
    refresh_token   TEXT,
    token_expiry    TIMESTAMP,
    smtp_host       VARCHAR(255),
    smtp_port       INTEGER,
    smtp_username   VARCHAR(255),
    smtp_password   VARCHAR(500),
    is_default      BOOLEAN      NOT NULL DEFAULT false,
    connected       BOOLEAN      NOT NULL DEFAULT false,
    last_sync_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT false
);

-- 2. Email templates
CREATE TABLE email_templates (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    subject         VARCHAR(500) NOT NULL,
    body_html       TEXT         NOT NULL,
    body_text       TEXT,
    category        VARCHAR(100),
    variables       TEXT,                            -- JSON array of variable names
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    usage_count     INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT false
);

-- 3. Email messages (sent / received / logged)
CREATE TABLE email_messages (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(50)  NOT NULL,
    account_id          UUID         REFERENCES email_accounts(id),
    from_address        VARCHAR(255) NOT NULL,
    to_addresses        VARCHAR(2000) NOT NULL,
    cc_addresses        VARCHAR(2000),
    bcc_addresses       VARCHAR(2000),
    subject             VARCHAR(500),
    body_text           TEXT,
    body_html           TEXT,
    direction           VARCHAR(10)  NOT NULL,       -- INBOUND, OUTBOUND
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    thread_id           VARCHAR(255),
    provider_message_id VARCHAR(255),
    in_reply_to         VARCHAR(255),
    template_id         UUID         REFERENCES email_templates(id),
    related_entity_type VARCHAR(50),
    related_entity_id   UUID,
    has_attachments     BOOLEAN      NOT NULL DEFAULT false,
    opened              BOOLEAN      NOT NULL DEFAULT false,
    open_count          INTEGER      NOT NULL DEFAULT 0,
    click_count         INTEGER      NOT NULL DEFAULT 0,
    first_opened_at     TIMESTAMP,
    sent_at             TIMESTAMP,
    scheduled_at        TIMESTAMP,
    error_message       TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    deleted             BOOLEAN      NOT NULL DEFAULT false
);

-- 4. Email tracking events (open / click / bounce / delivery)
CREATE TABLE email_tracking_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID         NOT NULL REFERENCES email_messages(id),
    event_type      VARCHAR(20)  NOT NULL,           -- SENT, DELIVERED, OPENED, CLICKED, BOUNCED, UNSUBSCRIBED
    link_url        TEXT,
    user_agent      TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 5. Email schedules (send-later queue)
CREATE TABLE email_schedules (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    message_id      UUID         REFERENCES email_messages(id),
    template_id     UUID         REFERENCES email_templates(id),
    to_addresses    VARCHAR(2000) NOT NULL,
    cc_addresses    VARCHAR(2000),
    subject         VARCHAR(500) NOT NULL,
    body_html       TEXT         NOT NULL,
    body_text       TEXT,
    scheduled_at    TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP,
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT false
);

-- ── Indexes ─────────────────────────────────────────────────
CREATE INDEX idx_email_accounts_tenant     ON email_accounts(tenant_id)      WHERE deleted = false;
CREATE INDEX idx_email_templates_tenant    ON email_templates(tenant_id)     WHERE deleted = false;
CREATE INDEX idx_email_messages_tenant     ON email_messages(tenant_id)      WHERE deleted = false;
CREATE INDEX idx_email_messages_thread     ON email_messages(thread_id)      WHERE thread_id IS NOT NULL;
CREATE INDEX idx_email_messages_entity     ON email_messages(related_entity_type, related_entity_id);
CREATE INDEX idx_email_messages_status     ON email_messages(status);
CREATE INDEX idx_email_messages_direction  ON email_messages(tenant_id, direction) WHERE deleted = false;
CREATE INDEX idx_email_messages_scheduled  ON email_messages(scheduled_at)   WHERE status = 'QUEUED';
CREATE INDEX idx_tracking_message          ON email_tracking_events(message_id);
CREATE INDEX idx_tracking_type             ON email_tracking_events(event_type);
CREATE INDEX idx_tracking_created          ON email_tracking_events(created_at);
CREATE INDEX idx_email_schedules_pending   ON email_schedules(scheduled_at)  WHERE status = 'PENDING';
CREATE INDEX idx_email_schedules_tenant    ON email_schedules(tenant_id)     WHERE deleted = false;
