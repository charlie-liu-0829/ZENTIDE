CREATE TABLE IF NOT EXISTS ow_zentide_insight_feedback (
  feedback_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id VARCHAR(10) NOT NULL,
  insight_id BIGINT UNSIGNED NOT NULL,
  feedback_type VARCHAR(24) NOT NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(feedback_id),
  UNIQUE KEY uk_zentide_insight_feedback_owner_insight(owner_id,insight_id),
  KEY idx_zentide_insight_feedback_owner_type(owner_id,feedback_type),
  CONSTRAINT fk_zentide_insight_feedback_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_insight_feedback_insight FOREIGN KEY(insight_id) REFERENCES ow_zentide_insight(insight_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE ow_zentide_insight
  ADD KEY idx_zentide_insight_change_status(change_id,status);

UPDATE ow_platform_metadata
SET metadata_value='zentide-personal-insights-v1'
WHERE metadata_key='platform_phase';
