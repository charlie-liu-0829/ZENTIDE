CREATE TABLE IF NOT EXISTS ow_zentide_change_discussion (
  discussion_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  change_id BIGINT UNSIGNED NOT NULL,
  author_id VARCHAR(10) NOT NULL,
  parent_discussion_id BIGINT UNSIGNED NULL,
  evidence_id BIGINT UNSIGNED NULL,
  body TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(discussion_id),
  KEY idx_zentide_discussion_change_status(change_id,status,created_at),
  KEY idx_zentide_discussion_parent(parent_discussion_id),
  CONSTRAINT fk_zentide_discussion_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_discussion_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_discussion_parent FOREIGN KEY(parent_discussion_id) REFERENCES ow_zentide_change_discussion(discussion_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_discussion_evidence FOREIGN KEY(evidence_id) REFERENCES ow_zentide_evidence(evidence_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-change-centered-community-v1'
WHERE metadata_key='platform_phase';
