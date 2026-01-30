-- V12__create_interviewer_technologies_table.sql
DROP TABLE IF EXISTS interviewer_technologies CASCADE;

CREATE TABLE interviewer_technologies (
    id BIGSERIAL PRIMARY KEY,
    interviewer_id BIGINT NOT NULL,
    technology_id BIGINT NOT NULL,
    years_of_experience INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interviewer_tech_interviewer FOREIGN KEY (interviewer_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_interviewer_tech_technology FOREIGN KEY (technology_id)
        REFERENCES technologies(id) ON DELETE CASCADE,
    CONSTRAINT uq_interviewer_technology UNIQUE (interviewer_id, technology_id),
    CONSTRAINT chk_years_experience_positive CHECK (years_of_experience >= 0)
);

CREATE INDEX idx_interviewer_technologies_interviewer
    ON interviewer_technologies(interviewer_id, is_active);

CREATE INDEX idx_interviewer_technologies_technology
    ON interviewer_technologies(technology_id, is_active);

CREATE OR REPLACE FUNCTION update_interviewer_technologies_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_interviewer_technologies_updated_at
    BEFORE UPDATE ON interviewer_technologies
    FOR EACH ROW
    EXECUTE FUNCTION update_interviewer_technologies_updated_at();