CREATE TABLE IF NOT EXISTS ow_zentide_topic_source (
  topic_source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  topic_id BIGINT UNSIGNED NOT NULL,
  source_id BIGINT UNSIGNED NOT NULL,
  platform_key VARCHAR(32) NOT NULL,
  observation_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_ADAPTER',
  added_by VARCHAR(10) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(topic_source_id),
  UNIQUE KEY uk_zentide_topic_source(topic_id,source_id),
  KEY idx_zentide_topic_source_platform(platform_key,observation_status),
  CONSTRAINT fk_zentide_topic_source_topic FOREIGN KEY(topic_id) REFERENCES ow_zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_source_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_source_user FOREIGN KEY(added_by) REFERENCES user_info(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_topic_follow (
  topic_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  notification_mode VARCHAR(20) NOT NULL DEFAULT 'DAILY',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(topic_id,user_id),
  KEY idx_zentide_topic_follow_user(user_id,created_at),
  CONSTRAINT fk_zentide_topic_follow_topic FOREIGN KEY(topic_id) REFERENCES ow_zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_follow_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_topic_change (
  topic_id BIGINT UNSIGNED NOT NULL,
  change_id BIGINT UNSIGNED NOT NULL,
  assigned_by VARCHAR(20) NOT NULL DEFAULT 'SOURCE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(topic_id,change_id),
  KEY idx_zentide_topic_change_feed(topic_id,created_at),
  CONSTRAINT fk_zentide_topic_change_topic FOREIGN KEY(topic_id) REFERENCES ow_zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_change_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-topic-community-foundation'
WHERE metadata_key='platform_phase';
