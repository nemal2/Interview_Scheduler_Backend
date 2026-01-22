-- V7__fix_designation_tier_foreign_key.sql
-- Fix the foreign key constraint that's pointing to wrong table

-- Step 1: Drop the incorrect foreign key constraint
ALTER TABLE designations
DROP CONSTRAINT IF EXISTS designations_tier_id_fkey CASCADE;

-- Step 2: Clean up designations pointing to non-existent tiers
UPDATE designations
SET tier_id = NULL, is_active = false
WHERE tier_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM tiers WHERE tiers.id = designations.tier_id);

-- Step 3: Add the correct foreign key constraint pointing to 'tiers' table
ALTER TABLE designations
ADD CONSTRAINT designations_tier_id_fkey
FOREIGN KEY (tier_id) REFERENCES tiers(id) ON DELETE SET NULL;

-- Step 4: Drop designation_tiers table if it exists
DROP TABLE IF EXISTS designation_tiers CASCADE;