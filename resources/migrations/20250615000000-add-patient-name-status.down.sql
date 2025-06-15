DROP INDEX IF EXISTS idx_patient_assessments_patient_name;
--;;
DROP INDEX IF EXISTS idx_patient_assessments_status;
--;;
ALTER TABLE patient_assessments DROP COLUMN patient_name;
--;;
ALTER TABLE patient_assessments DROP COLUMN assessment_status;
