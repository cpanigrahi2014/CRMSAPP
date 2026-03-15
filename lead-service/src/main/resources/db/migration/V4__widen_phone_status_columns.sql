-- Widen phone and status columns to support longer values (e.g. international phone numbers)
ALTER TABLE leads ALTER COLUMN phone TYPE VARCHAR(50);
ALTER TABLE leads ALTER COLUMN status TYPE VARCHAR(50);
ALTER TABLE leads ALTER COLUMN source TYPE VARCHAR(50);
