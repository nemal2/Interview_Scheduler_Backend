-- Step 1: Drop any incorrect constraints on designations table
DROP INDEX IF EXISTS idx_unique_dept_tier_order;
ALTER TABLE designations DROP CONSTRAINT IF EXISTS idx_unique_dept_tier_order;

-- Step 2: Fix duplicate data by reassigning level_order for duplicates
-- Find and fix duplicates within each tier
WITH numbered_designations AS (
    SELECT
        id,
        tier_id,
        level_order,
        ROW_NUMBER() OVER (PARTITION BY tier_id, level_order ORDER BY id) as rn,
        ROW_NUMBER() OVER (PARTITION BY tier_id ORDER BY level_order, id) as new_order
    FROM designations
    WHERE tier_id IS NOT NULL
)
UPDATE designations d
SET level_order = nd.new_order
FROM numbered_designations nd
WHERE d.id = nd.id
  AND nd.rn > 1; -- Only update duplicates

-- Step 3: Resequence all level_order values to ensure no gaps
WITH reordered AS (
    SELECT
        id,
        tier_id,
        ROW_NUMBER() OVER (PARTITION BY tier_id ORDER BY level_order, id) as new_level_order
    FROM designations
    WHERE tier_id IS NOT NULL
)
UPDATE designations d
SET level_order = r.new_level_order
FROM reordered r
WHERE d.id = r.id;

-- Step 4: Add the correct unique constraint for designations
-- This ensures no duplicate level_order within the same tier
ALTER TABLE designations
ADD CONSTRAINT unique_designation_tier_level
UNIQUE (tier_id, level_order);

-- Step 5: Optionally, ensure unique names within a department
-- Only add if this doesn't conflict with existing data
DO $$
BEGIN
    -- Check if there are duplicate names in the same department
    IF NOT EXISTS (
        SELECT 1
        FROM designations
        WHERE department_id IS NOT NULL
        GROUP BY department_id, name
        HAVING COUNT(*) > 1
    ) THEN
        ALTER TABLE designations
        ADD CONSTRAINT unique_designation_dept_name
        UNIQUE (department_id, name);
    ELSE
        RAISE NOTICE 'Skipping unique_designation_dept_name constraint due to duplicate names in same department';
    END IF;
END$$;

-- Step 6: Verify tiers table has correct constraint (should already exist from V4)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'tiers_department_id_tier_order_key'
    ) THEN
        ALTER TABLE tiers
        ADD CONSTRAINT tiers_department_id_tier_order_key
        UNIQUE (department_id, tier_order);
    END IF;
END$$;