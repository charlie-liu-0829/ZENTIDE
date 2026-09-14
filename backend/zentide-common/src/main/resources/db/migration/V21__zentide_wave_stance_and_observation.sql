CREATE TABLE IF NOT EXISTS ow_zentide_signal (
  signal_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  document_version_id BIGINT UNSIGNED NOT NULL,
  linked_change_id BIGINT UNSIGNED NULL,
  signal_key VARCHAR(255) NOT NULL,
  signal_type VARCHAR(40) NOT NULL,
  title VARCHAR(300) NOT NULL,
  summary TEXT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'OBSERVING',
  confidence_score DECIMAL(5,2) NOT NULL DEFAULT 50.00,
  discovered_at DATETIME(3) NOT NULL,
  promoted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(signal_id),
  UNIQUE KEY uk_zentide_signal_key(signal_key),
  UNIQUE KEY uk_zentide_signal_document_version(document_version_id),
  KEY idx_zentide_signal_status_time(status,discovered_at),
  KEY idx_zentide_signal_source(source_id,discovered_at),
  KEY idx_zentide_signal_change(linked_change_id),
  CONSTRAINT fk_zentide_signal_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id),
  CONSTRAINT fk_zentide_signal_document_version FOREIGN KEY(document_version_id) REFERENCES ow_zentide_document_version(document_version_id),
  CONSTRAINT fk_zentide_signal_change FOREIGN KEY(linked_change_id) REFERENCES ow_zentide_change(change_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_signal_topic (
  signal_id BIGINT UNSIGNED NOT NULL,
  topic_id BIGINT UNSIGNED NOT NULL,
  assigned_by VARCHAR(20) NOT NULL DEFAULT 'SOURCE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(signal_id,topic_id),
  KEY idx_zentide_signal_topic_topic(topic_id,created_at),
  CONSTRAINT fk_zentide_signal_topic_signal FOREIGN KEY(signal_id) REFERENCES ow_zentide_signal(signal_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_signal_topic_topic FOREIGN KEY(topic_id) REFERENCES ow_zentide_topic(topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_change_stance (
  change_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  stance_type VARCHAR(24) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(change_id,user_id),
  KEY idx_zentide_change_stance_type(change_id,stance_type),
  KEY idx_zentide_change_stance_user(user_id,updated_at),
  CONSTRAINT fk_zentide_change_stance_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_change_stance_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_observation_activity (
  activity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  signal_id BIGINT UNSIGNED NULL,
  change_id BIGINT UNSIGNED NULL,
  source_id BIGINT UNSIGNED NULL,
  stage VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  message VARCHAR(300) NOT NULL,
  detail_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(activity_id),
  KEY idx_zentide_observation_activity_time(created_at),
  KEY idx_zentide_observation_activity_signal(signal_id,created_at),
  KEY idx_zentide_observation_activity_change(change_id,created_at),
  CONSTRAINT fk_zentide_observation_activity_signal FOREIGN KEY(signal_id) REFERENCES ow_zentide_signal(signal_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_observation_activity_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_observation_activity_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-wave-stance-and-observation'
WHERE metadata_key='platform_phase';
