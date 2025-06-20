ALTER TABLE patient_assessments ADD COLUMN checkin_time TEXT;
--;;
CREATE INDEX IF NOT EXISTS idx_patient_assessments_checkin ON patient_assessments(checkin_time);
