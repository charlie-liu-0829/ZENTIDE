CREATE TABLE IF NOT EXISTS ow_application_record (
  application_record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id VARCHAR(10) NOT NULL,job_id BIGINT UNSIGNED NOT NULL,
  tracked_job_version_id BIGINT UNSIGNED NULL,status VARCHAR(24) NOT NULL DEFAULT 'DISCOVERED',notes TEXT NULL,
  saved_at DATETIME(3) NULL,applied_at DATETIME(3) NULL,completed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),lock_version INT NOT NULL DEFAULT 0,
  PRIMARY KEY(application_record_id),UNIQUE KEY uk_application_record_user_job(user_id,job_id),
  KEY idx_application_record_user_status(user_id,status,updated_at),KEY idx_application_record_job_status(job_id,status),
  CONSTRAINT fk_application_record_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_application_record_job FOREIGN KEY(job_id) REFERENCES ow_job(job_id) ON DELETE CASCADE,
  CONSTRAINT fk_application_record_version FOREIGN KEY(tracked_job_version_id) REFERENCES ow_job_version(job_version_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_application_status_history (
  history_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,application_record_id BIGINT UNSIGNED NOT NULL,from_status VARCHAR(24) NULL,to_status VARCHAR(24) NOT NULL,
  note VARCHAR(1000) NULL,changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(history_id),
  KEY idx_application_history_record_time(application_record_id,changed_at),
  CONSTRAINT fk_application_history_record FOREIGN KEY(application_record_id) REFERENCES ow_application_record(application_record_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata(metadata_key,metadata_value) VALUES('platform_phase','application-board')
ON DUPLICATE KEY UPDATE metadata_value=VALUES(metadata_value);
