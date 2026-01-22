-- V6__fix_active_only_constraints.sql
-- This migration fixes the constraint issues to only check active records

-- Step 1: Drop existing constraints that don't account for is_active
ALTER TABLE designations DROP CONSTRAINT IF EXISTS unique_designation_tier_level;
ALTER TABLE designations DROP CONSTRAINT IF EXISTS unique_designation_dept_name;
ALTER TABLE tiers DROP CONSTRAINT IF EXISTS tiers_department_id_tier_order_key;

-- Step 2: Create partial unique indexes that only apply to active records
-- For tiers: unique (department_id, tier_order) WHERE is_active = true
CREATE UNIQUE INDEX IF NOT EXISTS idx_tiers_dept_order_active
ON tiers (department_id, tier_order)
WHERE is_active = true;

-- For designations: unique (tier_id, level_order) WHERE is_active = true
CREATE UNIQUE INDEX IF NOT EXISTS idx_designations_tier_level_active
ON designations (tier_id, level_order)
WHERE is_active = true;

-- Step 3: Optional - unique name per department for active records only
CREATE UNIQUE INDEX IF NOT EXISTS idx_designations_dept_name_active
ON designations (department_id, name)
WHERE is_active = true;

-- Step 4: Clean up any existing duplicates in active records
-- For tiers
WITH tier_duplicates AS (
    SELECT
        id,
        department_id,
        tier_order,
        ROW_NUMBER() OVER (PARTITION BY department_id, tier_order ORDER BY created_at DESC, id DESC) as rn
    FROM tiers
    WHERE is_active = true
)
UPDATE tiers t
SET tier_order = (
    SELECT COALESCE(MAX(tier_order), 0) + td.rn
    FROM tiers t2
    WHERE t2.department_id = t.department_id
    AND t2.is_active = true
    AND t2.id < t.id
)
FROM tier_duplicates td
WHERE t.id = td.id AND td.rn > 1;

-- For designations
WITH designation_duplicates AS (
    SELECT
        id,
        tier_id,
        level_order,
        ROW_NUMBER() OVER (PARTITION BY tier_id, level_order ORDER BY id) as rn
    FROM designations
    WHERE is_active = true AND tier_id IS NOT NULL
)
UPDATE designations d
SET level_order = (
    SELECT COALESCE(MAX(level_order), 0) + dd.rn
    FROM designations d2
    WHERE d2.tier_id = d.tier_id
    AND d2.is_active = true
    AND d2.id < d.id
)
FROM designation_duplicates dd
WHERE d.id = dd.id AND dd.rn > 1;

-- Step 5: Add helpful indexes for performance
CREATE INDEX IF NOT EXISTS idx_tiers_department_active ON tiers (department_id, is_active);
CREATE INDEX IF NOT EXISTS idx_designations_tier_active ON designations (tier_id, is_active);
CREATE INDEX IF NOT EXISTS idx_designations_dept_active ON designations (department_id, is_active);