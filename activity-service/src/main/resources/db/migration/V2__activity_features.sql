-- V2: Add EMAIL type support, reminder fields, recurring task fields, location/notes for meetings/calls

-- Add new columns for reminders
ALTER TABLE activities ADD COLUMN IF NOT EXISTS reminder_at TIMESTAMP;
ALTER TABLE activities ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN NOT NULL DEFAULT false;

-- Add recurring task fields
ALTER TABLE activities ADD COLUMN IF NOT EXISTS recurrence_rule VARCHAR(50);  -- DAILY, WEEKLY, BIWEEKLY, MONTHLY
ALTER TABLE activities ADD COLUMN IF NOT EXISTS recurrence_end DATE;
ALTER TABLE activities ADD COLUMN IF NOT EXISTS parent_activity_id UUID REFERENCES activities(id);

-- Add location (for meetings) and notes (for calls/emails)
ALTER TABLE activities ADD COLUMN IF NOT EXISTS location VARCHAR(500);
ALTER TABLE activities ADD COLUMN IF NOT EXISTS call_duration_minutes INTEGER;
ALTER TABLE activities ADD COLUMN IF NOT EXISTS call_outcome VARCHAR(50);
ALTER TABLE activities ADD COLUMN IF NOT EXISTS email_to VARCHAR(500);
ALTER TABLE activities ADD COLUMN IF NOT EXISTS email_cc VARCHAR(500);

-- Index for reminder scheduler
CREATE INDEX IF NOT EXISTS idx_activities_reminder ON activities(reminder_at) WHERE reminder_sent = false AND deleted = false;

-- Index for recurring tasks
CREATE INDEX IF NOT EXISTS idx_activities_recurrence ON activities(recurrence_rule) WHERE recurrence_rule IS NOT NULL AND deleted = false;

-- Index for parent-child
CREATE INDEX IF NOT EXISTS idx_activities_parent ON activities(parent_activity_id) WHERE parent_activity_id IS NOT NULL;
