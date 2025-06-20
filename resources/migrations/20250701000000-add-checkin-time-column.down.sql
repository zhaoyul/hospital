DROP INDEX IF EXISTS idx_patient_assessments_checkin;
--;;
ALTER TABLE patient_assessments DROP COLUMN checkin_time;
