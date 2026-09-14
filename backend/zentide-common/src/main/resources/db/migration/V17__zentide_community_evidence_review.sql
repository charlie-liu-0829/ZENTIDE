CREATE TABLE IF NOT EXISTS ow_zentide_evidence_submission (
  submission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  change_id BIGINT UNSIGNED NOT NULL,
  author_id VARCHAR(10) NOT NULL,
  source_url VARCHAR(2048) NOT NULL,
  excerpt TEXT NOT NULL,
  note VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
  reviewed_by VARCHAR(100) NULL,
  review_note VARCHAR(500) NULL,
  accepted_evidence_id BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  reviewed_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(submission_id),
  KEY idx_zentide_evidence_submission_review(status,created_at),
  KEY idx_zentide_evidence_submission_change(change_id,status),
  CONSTRAINT fk_zentide_evidence_submission_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_evidence_submission_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_evidence_submission_evidence FOREIGN KEY(accepted_evidence_id) REFERENCES ow_zentide_evidence(evidence_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-community-evidence-review-v1'
WHERE metadata_key='platform_phase';
