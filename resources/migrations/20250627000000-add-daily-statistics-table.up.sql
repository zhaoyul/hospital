CREATE TABLE IF NOT EXISTS daily_statistics (
  date TEXT PRIMARY KEY,
  data TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME
);
--;;
CREATE INDEX IF NOT EXISTS idx_daily_statistics_date ON daily_statistics(date);
