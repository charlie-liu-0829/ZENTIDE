CREATE TABLE IF NOT EXISTS ow_zentide_change_watch (
  change_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  watch_mode VARCHAR(20) NOT NULL DEFAULT 'FOLLOW_UP',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(change_id, user_id),
  KEY idx_zentide_change_watch_user(user_id, status, updated_at),
  CONSTRAINT fk_zentide_change_watch_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_change_watch_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-change-watch'
WHERE metadata_key='platform_phase';
