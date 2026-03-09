-- V18__add_lookback_indexes.sql
--
-- PURPOSE:
--   The 30-day lookback queries introduced to fix data disappearing now scan
--   a wider date range. These indexes keep those scans fast even with large
--   datasets.
-- ─────────────────────────────────────────────────────────────────────────────

-- Covers: findAllActiveSlotsForHR (lookback), findByInterviewerIdAndIsActiveTrueWithLookback
-- The partial index (is_active = true) keeps it lean.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_availability_active_start_status'
    ) THEN
        CREATE INDEX idx_availability_active_start_status
            ON availability_slots (start_date_time, status)
            WHERE is_active = true;
    END IF;
END $$;

-- Covers the interviewer-scoped lookback query in AvailabilityService
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_availability_interviewer_start_active'
    ) THEN
        CREATE INDEX idx_availability_interviewer_start_active
            ON availability_slots (interviewer_id, start_date_time)
            WHERE is_active = true;
    END IF;
END $$;

-- Covers countAvailableSlotsFrom / countBookedSlotsFrom stat queries
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_availability_stats'
    ) THEN
        CREATE INDEX idx_availability_stats
            ON availability_slots (interviewer_id, status, start_date_time)
            WHERE is_active = true;
    END IF;
END $$;