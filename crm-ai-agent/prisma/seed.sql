-- Seed default CRM objects and a Sales pipeline so the agent has something to work with

INSERT INTO crm_objects (id, name, label, plural_label, icon, is_system, is_active, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'Lead',        'Lead',        'Leads',         'PersonSearch',  true, true, now(), now()),
  (gen_random_uuid(), 'Account',     'Account',     'Accounts',      'Business',      true, true, now(), now()),
  (gen_random_uuid(), 'Contact',     'Contact',     'Contacts',      'Contacts',      true, true, now(), now()),
  (gen_random_uuid(), 'Opportunity', 'Opportunity', 'Opportunities', 'TrendingUp',    true, true, now(), now()),
  (gen_random_uuid(), 'Activity',    'Activity',    'Activities',    'Event',         true, true, now(), now()),
  (gen_random_uuid(), 'Case',        'Case',        'Cases',         'Support',       true, true, now(), now()),
  (gen_random_uuid(), 'Campaign',    'Campaign',    'Campaigns',     'Campaign',      true, true, now(), now())
ON CONFLICT (name) DO NOTHING;

-- Default Sales Pipeline
INSERT INTO pipelines (id, name, object_name, description, is_default, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'Sales Pipeline', 'Opportunity', 'Default sales pipeline', true, true, now(), now())
ON CONFLICT (name) DO NOTHING;

-- Default roles
INSERT INTO roles (id, name, description, is_system, is_active, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'Admin',        'Full system access',          true, true, now(), now()),
  (gen_random_uuid(), 'Sales Manager','Manage sales team and data',  true, true, now(), now()),
  (gen_random_uuid(), 'Sales Rep',    'Standard sales user',         true, true, now(), now()),
  (gen_random_uuid(), 'Read Only',    'View-only access',            true, true, now(), now())
ON CONFLICT (name) DO NOTHING;
