-- BigQuery Migration: Phase 2.5 - Visa Sponsorship Detail
-- This migration adds a new JSON column for detailed visa sponsorship info 
-- and backfills it from the existing boolean column.

-- 1. Add new JSON column to raw_companies (use your actual dataset name)
ALTER TABLE `your-project.your-dataset.raw_companies` 
ADD COLUMN visa_sponsorship_detail JSON;

-- 2. Backfill: convert existing boolean to JSON object
UPDATE `your-project.your-dataset.raw_companies`
SET visa_sponsorship_detail = JSON_OBJECT('offered', visa_sponsorship)
WHERE visa_sponsorship IS NOT NULL;

-- 3. (Optional) Fix inconsistent constant naming if you haven't already
-- The backend now uses CompanyFields.VISA_SPONSORSHIP_DETAIL to point to this column.
