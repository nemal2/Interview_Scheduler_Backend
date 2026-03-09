-- ============================================================
-- Migration: add extra fields to candidates table
-- V19__candidate_table_update.sql
--   • jd_url           – Job Description URL
--   • job_reference_code – Internal requisition code (REQ-2024-001)
--   • location         – Candidate's location / city
-- Also ensures the email column has a UNIQUE constraint
-- (idempotent — uses IF NOT EXISTS / DO blocks).
-- ============================================================

ALTER TABLE candidates
    ADD COLUMN IF NOT EXISTS jd_url             VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS job_reference_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location           VARCHAR(255);

-- Ensure email is unique (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'candidates'::regclass
           AND conname   = 'candidates_email_key'
           AND contype   = 'u'
    ) THEN
        ALTER TABLE candidates ADD CONSTRAINT candidates_email_key UNIQUE (email);
    END IF;
END$$;