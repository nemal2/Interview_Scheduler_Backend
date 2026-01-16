-- Add department_id and description to designations table
ALTER TABLE designations ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id);
ALTER TABLE designations ADD COLUMN IF NOT EXISTS description VARCHAR(500);

-- Update existing sample data
UPDATE designations SET department_id = (SELECT id FROM departments WHERE code = 'ENG' LIMIT 1)
WHERE name IN ('Software Engineer', 'Senior Software Engineer', 'Tech Lead', 'Senior Tech Lead', 'Architect');

UPDATE designations SET department_id = (SELECT id FROM departments WHERE code = 'HR' LIMIT 1)
WHERE name IN ('HR Manager', 'Senior HR Manager');

-- Add descriptions
UPDATE designations SET description = 'Entry level developer position' WHERE name = 'Software Engineer';
UPDATE designations SET description = 'Experienced software developer' WHERE name = 'Senior Software Engineer';
UPDATE designations SET description = 'Technical leadership role' WHERE name = 'Tech Lead';
UPDATE designations SET description = 'Senior technical leadership' WHERE name = 'Senior Tech Lead';
UPDATE designations SET description = 'Architecture and design lead' WHERE name = 'Architect';
UPDATE designations SET description = 'Manages HR operations' WHERE name = 'HR Manager';
UPDATE designations SET description = 'Senior HR management role' WHERE name = 'Senior HR Manager';