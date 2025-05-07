CREATE TABLE IF NOT EXISTS patient_assessments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  patient_id TEXT NOT NULL,
  assessment_data TEXT NOT NULL,  -- 或 JSON，如果你的 SQLite 版本支持
  patient_name_pinyin TEXT, -- 新增：患者姓名拼音
  patient_name_initial TEXT, -- 新增：患者姓名首字母
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME
);
--;;
CREATE INDEX idx_patient_assessments_patient_id ON patient_assessments(patient_id);
--;;
CREATE INDEX idx_patient_assessments_name_pinyin ON patient_assessments(patient_name_pinyin);
--;;
CREATE INDEX idx_patient_assessments_name_initial ON patient_assessments(patient_name_initial);
--;;
CREATE INDEX idx_patient_assessments_updated_at ON patient_assessments(updated_at);
