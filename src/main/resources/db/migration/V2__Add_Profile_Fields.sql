-- Add bio and yearsOfExperience columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(1000);
ALTER TABLE users ADD COLUMN IF NOT EXISTS years_of_experience INTEGER;

-- Insert sample technologies
INSERT INTO technologies (name, category, is_active) VALUES
('Java', 'Programming Language', true),
('Spring Boot', 'Framework', true),
('React', 'Framework', true),
('TypeScript', 'Programming Language', true),
('AWS', 'Cloud Platform', true),
('Docker', 'DevOps', true),
('Microservices', 'Architecture', true),
('Python', 'Programming Language', true),
('Node.js', 'Runtime', true),
('Angular', 'Framework', true),
('Vue.js', 'Framework', true),
('PostgreSQL', 'Database', true),
('MongoDB', 'Database', true),
('Redis', 'Cache', true),
('Kubernetes', 'DevOps', true),
('System Design', 'Concept', true)
ON CONFLICT (name) DO NOTHING;

-- Insert sample departments if not exists
INSERT INTO departments (name, code) VALUES
('Engineering', 'ENG'),
('Human Resources', 'HR'),
('Finance', 'FIN'),
('Marketing', 'MKT'),
('Operations', 'OPS')
ON CONFLICT (code) DO NOTHING;

-- Insert sample designations if not exists
INSERT INTO designations (name, hierarchy_level, is_active) VALUES
('Software Engineer', 1, true),
('Senior Software Engineer', 2, true),
('Tech Lead', 3, true),
('Senior Tech Lead', 4, true),
('Architect', 5, true),
('Principal Engineer', 6, true),
('Engineering Manager', 7, true),
('Director of Engineering', 8, true),
('HR Manager', 2, true),
('Senior HR Manager', 3, true)
ON CONFLICT DO NOTHING;