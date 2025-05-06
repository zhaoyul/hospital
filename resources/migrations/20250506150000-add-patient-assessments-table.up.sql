CREATE TABLE IF NOT EXISTS patient_assessments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  patient_id TEXT NOT NULL,
  assessment_data TEXT NOT NULL,  -- 或 JSON，如果你的 SQLite 版本支持
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME
);
--;;
CREATE INDEX idx_patient_assessments_patient_id ON patient_assessments(patient_id);
