-- V9__create_candidates_table.sql

CREATE TABLE candidates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    department_id BIGINT,
    target_designation_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    resume_url VARCHAR(2000),
    notes TEXT,
    years_of_experience INTEGER,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT fk_candidate_department FOREIGN KEY (department_id)
        REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT fk_candidate_designation FOREIGN KEY (target_designation_id)
        REFERENCES designations(id) ON DELETE SET NULL
);

-- Create unique index for active candidates with same email
CREATE UNIQUE INDEX idx_candidates_email_active
    ON candidates (email)
    WHERE is_active = true;

-- Create indexes for common queries
CREATE INDEX idx_candidates_department ON candidates (department_id, is_active);
CREATE INDEX idx_candidates_status ON candidates (status, is_active);
CREATE INDEX idx_candidates_designation ON candidates (target_designation_id, is_active);
CREATE INDEX idx_candidates_applied_at ON candidates (applied_at DESC);

-- Add search index for name and email
CREATE INDEX idx_candidates_search ON candidates USING gin(
    to_tsvector('english', name || ' ' || COALESCE(email, ''))
) WHERE is_active = true;