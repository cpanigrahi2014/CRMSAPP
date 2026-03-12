-- Contact Service: V2 – All 10 features
-- Feature 4: Social profile fields
-- Feature 5: Segmentation
-- Feature 6: Marketing consent tracking
-- Feature 2: Enhanced account linking (owner_id)

ALTER TABLE contacts ADD COLUMN IF NOT EXISTS lead_source VARCHAR(50);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS lifecycle_stage VARCHAR(50) DEFAULT 'SUBSCRIBER';
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS segment VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS owner_id UUID;

-- Social profiles (Feature 4)
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(500);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS twitter_url VARCHAR(500);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(500);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS other_social_url VARCHAR(500);

-- Marketing consent (Feature 6)
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS email_opt_in BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS sms_opt_in BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS phone_opt_in BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS consent_date TIMESTAMP;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS consent_source VARCHAR(100);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS do_not_call BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_contacts_segment ON contacts(segment);
CREATE INDEX IF NOT EXISTS idx_contacts_lifecycle ON contacts(lifecycle_stage);
CREATE INDEX IF NOT EXISTS idx_contacts_owner ON contacts(owner_id);

-- Feature 8: Tagging
CREATE TABLE IF NOT EXISTS contact_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    tag_name VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(contact_id, tag_name)
);

CREATE INDEX IF NOT EXISTS idx_contact_tags_contact ON contact_tags(contact_id);
CREATE INDEX IF NOT EXISTS idx_contact_tags_tenant ON contact_tags(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contact_tags_name ON contact_tags(tag_name);

-- Feature 3: Communication history
CREATE TABLE IF NOT EXISTS contact_communications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    comm_type VARCHAR(30) NOT NULL,
    subject VARCHAR(500),
    body TEXT,
    direction VARCHAR(20) NOT NULL DEFAULT 'OUTBOUND',
    status VARCHAR(30) DEFAULT 'COMPLETED',
    communication_date TIMESTAMP NOT NULL DEFAULT NOW(),
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_contact_comms_contact ON contact_communications(contact_id);
CREATE INDEX IF NOT EXISTS idx_contact_comms_tenant ON contact_communications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contact_comms_date ON contact_communications(communication_date DESC);

-- Feature 7: Activity timeline
CREATE TABLE IF NOT EXISTS contact_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    description TEXT,
    metadata TEXT,
    tenant_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_contact_activities_contact ON contact_activities(contact_id);
CREATE INDEX IF NOT EXISTS idx_contact_activities_tenant ON contact_activities(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contact_activities_date ON contact_activities(created_at DESC);
