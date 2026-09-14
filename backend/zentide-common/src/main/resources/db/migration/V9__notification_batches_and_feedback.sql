CREATE TABLE IF NOT EXISTS ow_notification_batch (
  batch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,batch_key VARCHAR(128) NOT NULL,user_id VARCHAR(10) NOT NULL,
  delivery_mode VARCHAR(20) NOT NULL,status VARCHAR(24) NOT NULL DEFAULT 'BUILDING',item_count INT UNSIGNED NOT NULL DEFAULT 0,
  title VARCHAR(300) NOT NULL,content TEXT NOT NULL,digest_notification_id BIGINT UNSIGNED NULL,scheduled_at DATETIME(3) NULL,
  sent_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(batch_id),UNIQUE KEY uk_notification_batch_key(batch_key),KEY idx_notification_batch_user_created(user_id,created_at),
  KEY idx_notification_batch_status_scheduled(status,scheduled_at),KEY idx_notification_batch_digest(digest_notification_id),
  CONSTRAINT fk_notification_batch_digest FOREIGN KEY(digest_notification_id) REFERENCES ow_notification(notification_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_notification_batch_item (
  batch_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,batch_id BIGINT UNSIGNED NOT NULL,notification_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(batch_item_id),
  UNIQUE KEY uk_notification_batch_item(batch_id,notification_id),UNIQUE KEY uk_notification_item_notification(notification_id),
  CONSTRAINT fk_notification_batch_item_batch FOREIGN KEY(batch_id) REFERENCES ow_notification_batch(batch_id) ON DELETE CASCADE,
  CONSTRAINT fk_notification_batch_item_notification FOREIGN KEY(notification_id) REFERENCES ow_notification(notification_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_notification_feedback (
  feedback_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id VARCHAR(10) NOT NULL,notification_id BIGINT UNSIGNED NOT NULL,
  feedback_type VARCHAR(24) NOT NULL,note VARCHAR(500) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(feedback_id),
  UNIQUE KEY uk_notification_feedback_user_notification(user_id,notification_id),KEY idx_notification_feedback_type_created(feedback_type,created_at),
  CONSTRAINT fk_notification_feedback_notification FOREIGN KEY(notification_id) REFERENCES ow_notification(notification_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata(metadata_key,metadata_value) VALUES('platform_phase','notification-batches-and-feedback')
ON DUPLICATE KEY UPDATE metadata_value=VALUES(metadata_value);
