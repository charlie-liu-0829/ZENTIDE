CREATE TABLE IF NOT EXISTS ow_zentide_briefing (
  briefing_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id VARCHAR(10) NOT NULL,
  briefing_date DATE NOT NULL,
  briefing_type VARCHAR(20) NOT NULL DEFAULT 'DAILY',
  headline VARCHAR(300) NOT NULL,
  body_json JSON NOT NULL,
  item_count INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(briefing_id),
  UNIQUE KEY uk_zentide_briefing_owner_date_type(owner_id,briefing_date,briefing_type),
  KEY idx_zentide_briefing_owner_time(owner_id,briefing_date DESC),
  CONSTRAINT fk_zentide_briefing_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-daily-briefing-v1'
WHERE metadata_key='platform_phase';
