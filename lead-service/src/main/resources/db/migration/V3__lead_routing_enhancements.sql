-- ================================================================
-- V3: Enhanced Lead Routing (Round-Robin + Capacity)
-- ================================================================

-- Add round-robin support fields to assignment rules
ALTER TABLE lead_assignment_rules ADD COLUMN IF NOT EXISTS
    assignment_type VARCHAR(30) DEFAULT 'DIRECT';  -- DIRECT, ROUND_ROBIN

ALTER TABLE lead_assignment_rules ADD COLUMN IF NOT EXISTS
    round_robin_members TEXT;  -- JSON array of user UUIDs for round-robin

ALTER TABLE lead_assignment_rules ADD COLUMN IF NOT EXISTS
    round_robin_index INTEGER DEFAULT 0;  -- current position in rotation
