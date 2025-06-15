CREATE TABLE IF NOT EXISTS consent_forms (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  assessment_id INTEGER NOT NULL UNIQUE,
  sedation_form TEXT,
  pre_anesthesia_form TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME,
  FOREIGN KEY (assessment_id) REFERENCES patient_assessments(id)
);
--;;
CREATE UNIQUE INDEX idx_consent_forms_assessment_id ON consent_forms(assessment_id);
