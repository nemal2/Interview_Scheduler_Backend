-- Create tiers table (safe)
CREATE TABLE IF NOT EXISTS tiers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    department_id BIGINT REFERENCES departments(id),
    tier_order INT NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (department_id, tier_order)
);

-- Add tier_id column safely
ALTER TABLE designations
ADD COLUMN IF NOT EXISTS tier_id BIGINT REFERENCES tiers(id);

-- Rename column safely
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'designations'
          AND column_name = 'hierarchy_level'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'designations'
          AND column_name = 'level_order'
    ) THEN
        ALTER TABLE designations
        RENAME COLUMN hierarchy_level TO level_order;
    END IF;
END$$;

-- Insert sample tiers only if not already present
INSERT INTO tiers (name, department_id, tier_order, description, is_active)
SELECT 'Entry Level', id, 1, 'Junior positions', true
FROM departments
WHERE code = 'ENG'
AND NOT EXISTS (
    SELECT 1 FROM tiers t
    WHERE t.department_id = departments.id AND t.tier_order = 1
)
UNION ALL
SELECT 'Mid Level', id, 2, 'Experienced professionals', true
FROM departments
WHERE code = 'ENG'
AND NOT EXISTS (
    SELECT 1 FROM tiers t
    WHERE t.department_id = departments.id AND t.tier_order = 2
)
UNION ALL
SELECT 'Senior Level', id, 3, 'Senior positions', true
FROM departments
WHERE code = 'ENG'
AND NOT EXISTS (
    SELECT 1 FROM tiers t
    WHERE t.department_id = departments.id AND t.tier_order = 3
)
UNION ALL
SELECT 'Leadership', id, 4, 'Management and leadership roles', true
FROM departments
WHERE code = 'ENG'
AND NOT EXISTS (
    SELECT 1 FROM tiers t
    WHERE t.department_id = departments.id AND t.tier_order = 4
);

-- Update designations safely
UPDATE designations d
SET tier_id = t.id
FROM tiers t
WHERE d.department_id = t.department_id
AND t.name = 'Entry Level'
AND d.name = 'Software Engineer'
AND d.tier_id IS NULL;

UPDATE designations d
SET tier_id = t.id
FROM tiers t
WHERE d.department_id = t.department_id
AND t.name = 'Mid Level'
AND d.name IN ('Senior Software Engineer', 'Tech Lead')
AND d.tier_id IS NULL;

UPDATE designations d
SET tier_id = t.id
FROM tiers t
WHERE d.department_id = t.department_id
AND t.name = 'Senior Level'
AND d.name IN ('Senior Tech Lead', 'Architect')
AND d.tier_id IS NULL;

UPDATE designations d
SET tier_id = t.id
FROM tiers t
WHERE d.department_id = t.department_id
AND t.name = 'Leadership'
AND d.name IN ('Engineering Manager', 'Director of Engineering')
AND d.tier_id IS NULL;
