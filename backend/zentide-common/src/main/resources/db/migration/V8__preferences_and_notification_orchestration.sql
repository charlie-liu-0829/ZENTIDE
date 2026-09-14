CREATE TABLE IF NOT EXISTS ow_user_preference (
  user_id VARCHAR(10) NOT NULL,target_roles JSON NULL,target_cities JSON NULL,job_types JSON NULL,
  education VARCHAR(32) NULL,graduation_year SMALLINT NULL,years_of_experience DECIMAL(4,1) NULL,
  skills JSON NULL,industries JSON NULL,accept_remote TINYINT(1) NOT NULL DEFAULT 0,
  accept_relocation TINYINT(1) NOT NULL DEFAULT 0,notification_frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',
  quiet_hours_start TIME NULL,quiet_hours_end TIME NULL,profile_version INT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(user_id),CONSTRAINT fk_user_preference_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata(metadata_key,metadata_value) VALUES('platform_phase','preference-notification-orchestration')
ON DUPLICATE KEY UPDATE metadata_value=VALUES(metadata_value);
