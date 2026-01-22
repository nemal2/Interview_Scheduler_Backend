-- V8__fix_active_only_constraints.sql
-- Fix unique constraints to only check active records

-- Step 1: Drop existing constraints that don't account for is_active
ALTER TABLE designations DROP CONSTRAINT IF EXISTS unique_designation_tier_level CASCADE;
ALTER TABLE designations DROP CONSTRAINT IF EXISTS unique_designation_dept_name CASCADE;
ALTER TABLE tiers DROP CONSTRAINT IF EXISTS tiers_department_id_tier_order_key CASCADE;

-- Step 2: Drop any existing partial indexes (use CASCADE to be safe)
DROP INDEX IF EXISTS idx_designations_dept_name_active CASCADE;
DROP INDEX IF EXISTS idx_designations_tier_level_active CASCADE;
DROP INDEX IF EXISTS idx_tiers_dept_order_active CASCADE;
DROP INDEX IF EXISTS idx_designations_tier_name_active CASCADE;
DROP INDEX IF EXISTS idx_tiers_department_active CASCADE;
DROP INDEX IF EXISTS idx_designations_tier_active CASCADE;
DROP INDEX IF EXISTS idx_designations_dept_active CASCADE;

-- Step 3: Create partial unique indexes that only apply to active records
-- For tiers: unique (department_id, tier_order) WHERE is_active = true
CREATE UNIQUE INDEX idx_tiers_dept_order_active
ON tiers (department_id, tier_order)
WHERE is_active = true;

-- For designations: unique (tier_id, level_order) WHERE is_active = true
CREATE UNIQUE INDEX idx_designations_tier_level_active
ON designations (tier_id, level_order)
WHERE is_active = true AND tier_id IS NOT NULL;

-- For designations: unique name per tier (not department-wide)
-- This allows same designation name in different tiers
CREATE UNIQUE INDEX idx_designations_tier_name_active
ON designations (tier_id, LOWER(name))
WHERE is_active = true AND tier_id IS NOT NULL;

-- Step 4: Add helpful indexes for performance
CREATE INDEX idx_tiers_department_active ON tiers (department_id, is_active);
CREATE INDEX idx_designations_tier_active ON designations (tier_id, is_active);
CREATE INDEX idx_designations_dept_active ON designations (department_id, is_active);