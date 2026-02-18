-- V15__add_interview_panels_and_slot_splitting.sql

-- Create interview_panels table (for panel interviews - multiple interviewers, one candidate)
CREATE TABLE IF NOT EXISTS interview_panels (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT REFERENCES candidates(id) ON DELETE SET NULL,
    candidate_name VARCHAR(255) NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    notes TEXT,
    requested_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    is_urgent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add panel_id to interview_requests (null for single-interviewer requests)
ALTER TABLE interview_requests
    ADD COLUMN IF NOT EXISTS panel_id BIGINT REFERENCES interview_panels(id) ON DELETE SET NULL;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_interview_panels_candidate ON interview_panels(candidate_id);
CREATE INDEX IF NOT EXISTS idx_interview_panels_dates ON interview_panels(start_date_time, end_date_time);
CREATE INDEX IF NOT EXISTS idx_interview_requests_panel ON interview_requests(panel_id);

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_interview_panels_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_interview_panels_updated_at
    BEFORE UPDATE ON interview_panels
    FOR EACH ROW
    EXECUTE FUNCTION update_interview_panels_updated_at();

COMMENT ON TABLE interview_panels IS 'Groups multiple interview requests for the same candidate at the same time (panel interviews)';
COMMENT ON COLUMN interview_requests.panel_id IS 'If set, this request is part of a panel interview session';