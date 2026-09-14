CREATE TABLE IF NOT EXISTS ow_zentide_topic (
  topic_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  canonical_name VARCHAR(120) NOT NULL,
  topic_type VARCHAR(32) NOT NULL DEFAULT 'TECHNOLOGY',
  aliases_json JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(topic_id),
  UNIQUE KEY uk_zentide_topic_name(canonical_name),
  KEY idx_zentide_topic_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_radar (
  radar_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id VARCHAR(10) NOT NULL,
  name VARCHAR(120) NOT NULL,
  visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  version INT UNSIGNED NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(radar_id),
  KEY idx_zentide_radar_owner_status(owner_id,status),
  CONSTRAINT fk_zentide_radar_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_radar_topic (
  radar_topic_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  radar_id BIGINT UNSIGNED NOT NULL,
  topic_id BIGINT UNSIGNED NOT NULL,
  weight DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  include_descendants TINYINT(1) NOT NULL DEFAULT 1,
  note VARCHAR(500) NULL,
  PRIMARY KEY(radar_topic_id),
  UNIQUE KEY uk_zentide_radar_topic(radar_id,topic_id),
  CONSTRAINT fk_zentide_radar_topic_radar FOREIGN KEY(radar_id) REFERENCES ow_zentide_radar(radar_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_radar_topic_topic FOREIGN KEY(topic_id) REFERENCES ow_zentide_topic(topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_change (
  change_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entity_type VARCHAR(32) NOT NULL,
  entity_key VARCHAR(255) NOT NULL,
  change_type VARCHAR(40) NOT NULL,
  title VARCHAR(300) NOT NULL,
  summary TEXT NOT NULL,
  importance VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  status VARCHAR(24) NOT NULL DEFAULT 'CANDIDATE',
  occurred_at DATETIME(3) NULL,
  published_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(change_id),
  KEY idx_zentide_change_status_time(status,occurred_at),
  KEY idx_zentide_change_entity(entity_type,entity_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_evidence (
  evidence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  change_id BIGINT UNSIGNED NOT NULL,
  source_name VARCHAR(200) NOT NULL,
  source_url VARCHAR(2048) NOT NULL,
  locator VARCHAR(500) NULL,
  excerpt TEXT NULL,
  trust_tier VARCHAR(24) NOT NULL DEFAULT 'PRIMARY',
  excerpt_hash CHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(evidence_id),
  KEY idx_zentide_evidence_change(change_id),
  CONSTRAINT fk_zentide_evidence_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_insight (
  insight_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id VARCHAR(10) NOT NULL,
  change_id BIGINT UNSIGNED NOT NULL,
  relevance_score DECIMAL(5,2) NOT NULL,
  title VARCHAR(300) NOT NULL,
  body TEXT NOT NULL,
  why_json JSON NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(insight_id),
  UNIQUE KEY uk_zentide_insight_owner_change(owner_id,change_id),
  KEY idx_zentide_insight_owner_status(owner_id,status),
  CONSTRAINT fk_zentide_insight_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_insight_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata(metadata_key,metadata_value) VALUES('platform_phase','zentide-radar-and-fact-foundation')
ON DUPLICATE KEY UPDATE metadata_value=VALUES(metadata_value);
