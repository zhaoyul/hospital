ALTER TABLE patient_assessments ADD COLUMN patient_name TEXT;
--;;
ALTER TABLE patient_assessments ADD COLUMN assessment_status TEXT;
--;;
CREATE INDEX IF NOT EXISTS idx_patient_assessments_patient_name ON patient_assessments(patient_name);
--;;
CREATE INDEX IF NOT EXISTS idx_patient_assessments_status ON patient_assessments(assessment_status);
