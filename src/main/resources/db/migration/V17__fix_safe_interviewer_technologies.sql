-- V17__fix_safe_interviewer_technologies.sql
--
-- PURPOSE:
--   V12 contains `DROP TABLE IF EXISTS interviewer_technologies CASCADE` which
--   will wipe ALL technology assignments if Flyway ever re-runs that migration
--   (e.g. after flyway:repair or a checksum mismatch).
--
--   This migration:
--     1. Ensures the table exists with all required columns (idempotent).
--     2. Ensures all constraints and indexes exist (idempotent).
--     3. Ensures the updated_at trigger exists (idempotent).
--
--   Because every statement uses IF NOT EXISTS / OR REPLACE, this migration
--   is fully safe to run multiple times without data loss.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Create table only if it was somehow dropped
CREATE TABLE IF NOT EXISTS interviewer_technologies (
    id                  BIGSERIAL    PRIMARY KEY,
    interviewer_id      BIGINT       NOT NULL,
    technology_id       BIGINT       NOT NULL,
    years_of_experience INTEGER      NOT NULL DEFAULT 0,
    is_active           BOOLEAN      NOT NULL DEFAULT true,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Foreign keys — add only if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_interviewer_tech_interviewer'
    ) THEN
        ALTER TABLE interviewer_technologies
        ADD CONSTRAINT fk_interviewer_tech_interviewer
            FOREIGN KEY (interviewer_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_interviewer_tech_technology'
    ) THEN
        ALTER TABLE interviewer_technologies
        ADD CONSTRAINT fk_interviewer_tech_technology
            FOREIGN KEY (technology_id) REFERENCES technologies(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_interviewer_technology'
    ) THEN
        ALTER TABLE interviewer_technologies
        ADD CONSTRAINT uq_interviewer_technology UNIQUE (interviewer_id, technology_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_years_experience_positive'
    ) THEN
        ALTER TABLE interviewer_technologies
        ADD CONSTRAINT chk_years_experience_positive
            CHECK (years_of_experience >= 0);
    END IF;
END $$;

-- 3. Indexes — create only if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_interviewer_technologies_interviewer'
    ) THEN
        CREATE INDEX idx_interviewer_technologies_interviewer
            ON interviewer_technologies(interviewer_id, is_active);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_interviewer_technologies_technology'
    ) THEN
        CREATE INDEX idx_interviewer_technologies_technology
            ON interviewer_technologies(technology_id, is_active);
    END IF;
END $$;

-- 4. updated_at trigger — OR REPLACE means this is always safe to run
CREATE OR REPLACE FUNCTION update_interviewer_technologies_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop and recreate trigger (triggers cannot use OR REPLACE in older Postgres)
DROP TRIGGER IF EXISTS trigger_interviewer_technologies_updated_at
    ON interviewer_technologies;

CREATE TRIGGER trigger_interviewer_technologies_updated_at
    BEFORE UPDATE ON interviewer_technologies
    FOR EACH ROW
    EXECUTE FUNCTION update_interviewer_technologies_updated_at();

-- 5. Verify data is intact — logs a warning if the table is suspiciously empty
DO $$
DECLARE
    row_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO row_count FROM interviewer_technologies;
    IF row_count = 0 THEN
        RAISE WARNING
            'interviewer_technologies table is empty — possible data loss from a previous V12 re-run. '
            'Restore from backup if interviewers are missing their technology assignments.';
    ELSE
        RAISE NOTICE 'interviewer_technologies: % rows intact.', row_count;
    END IF;
END $$;