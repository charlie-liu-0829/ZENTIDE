CREATE TABLE IF NOT EXISTS ow_zentide_source (
  source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_name VARCHAR(200) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  canonical_url VARCHAR(2048) NOT NULL,
  canonical_url_hash CHAR(64) NOT NULL,
  trust_tier VARCHAR(24) NOT NULL DEFAULT 'PRIMARY',
  adapter_key VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  policy_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(source_id),
  UNIQUE KEY uk_zentide_source_url_hash(canonical_url_hash),
  KEY idx_zentide_source_url(canonical_url(768)),
  KEY idx_zentide_source_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_fetch_attempt (
  attempt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  request_url VARCHAR(2048) NOT NULL,
  status VARCHAR(24) NOT NULL,
  http_status SMALLINT UNSIGNED NULL,
  content_type VARCHAR(160) NULL,
  response_hash CHAR(64) NULL,
  error_code VARCHAR(80) NULL,
  error_message VARCHAR(1000) NULL,
  started_at DATETIME(3) NOT NULL,
  finished_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(attempt_id),
  KEY idx_zentide_fetch_source_time(source_id,created_at),
  CONSTRAINT fk_zentide_fetch_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_raw_snapshot (
  raw_snapshot_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  attempt_id BIGINT UNSIGNED NOT NULL,
  captured_at DATETIME(3) NOT NULL,
  content_type VARCHAR(160) NULL,
  content_hash CHAR(64) NOT NULL,
  storage_ref VARCHAR(500) NULL,
  body MEDIUMTEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(raw_snapshot_id),
  UNIQUE KEY uk_zentide_snapshot_hash(source_id,content_hash),
  KEY idx_zentide_snapshot_time(source_id,captured_at),
  CONSTRAINT fk_zentide_snapshot_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id),
  CONSTRAINT fk_zentide_snapshot_attempt FOREIGN KEY(attempt_id) REFERENCES ow_zentide_fetch_attempt(attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_document (
  document_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  canonical_key VARCHAR(500) NOT NULL,
  current_version_no INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(document_id),
  UNIQUE KEY uk_zentide_document_key(source_id,canonical_key),
  CONSTRAINT fk_zentide_document_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_document_version (
  document_version_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  document_id BIGINT UNSIGNED NOT NULL,
  version_no INT UNSIGNED NOT NULL,
  raw_snapshot_id BIGINT UNSIGNED NOT NULL,
  content_hash CHAR(64) NOT NULL,
  semantic_hash CHAR(64) NULL,
  title VARCHAR(500) NULL,
  normalized_content MEDIUMTEXT NULL,
  published_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(document_version_id),
  UNIQUE KEY uk_zentide_document_version(document_id,version_no),
  UNIQUE KEY uk_zentide_document_content(document_id,content_hash),
  CONSTRAINT fk_zentide_document_version_document FOREIGN KEY(document_id) REFERENCES ow_zentide_document(document_id),
  CONSTRAINT fk_zentide_document_version_snapshot FOREIGN KEY(raw_snapshot_id) REFERENCES ow_zentide_raw_snapshot(raw_snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_tracked_entity (
  entity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NULL,
  entity_type VARCHAR(32) NOT NULL,
  canonical_key VARCHAR(500) NOT NULL,
  display_name VARCHAR(300) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(entity_id),
  UNIQUE KEY uk_zentide_entity_key(entity_type,canonical_key),
  KEY idx_zentide_entity_source(source_id),
  CONSTRAINT fk_zentide_entity_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE ow_zentide_change
  ADD COLUMN document_version_id BIGINT UNSIGNED NULL,
  ADD COLUMN entity_id BIGINT UNSIGNED NULL,
  ADD COLUMN change_key VARCHAR(500) NULL,
  ADD COLUMN what_happened TEXT NULL,
  ADD COLUMN verification_status VARCHAR(24) NOT NULL DEFAULT 'UNVERIFIED',
  ADD COLUMN corrected_by_change_id BIGINT UNSIGNED NULL,
  ADD COLUMN supersedes_change_id BIGINT UNSIGNED NULL,
  ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  ADD KEY idx_zentide_change_verification(verification_status,created_at),
  ADD KEY idx_zentide_change_document(document_version_id),
  ADD KEY idx_zentide_change_entity_id(entity_id),
  ADD UNIQUE KEY uk_zentide_change_key(change_key),
  ADD CONSTRAINT fk_zentide_change_document_version FOREIGN KEY(document_version_id) REFERENCES ow_zentide_document_version(document_version_id),
  ADD CONSTRAINT fk_zentide_change_entity FOREIGN KEY(entity_id) REFERENCES ow_zentide_tracked_entity(entity_id),
  ADD CONSTRAINT fk_zentide_change_correction FOREIGN KEY(corrected_by_change_id) REFERENCES ow_zentide_change(change_id),
  ADD CONSTRAINT fk_zentide_change_supersedes FOREIGN KEY(supersedes_change_id) REFERENCES ow_zentide_change(change_id);

CREATE TABLE IF NOT EXISTS ow_zentide_claim (
  claim_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  change_id BIGINT UNSIGNED NOT NULL,
  claim_type VARCHAR(24) NOT NULL DEFAULT 'FACT',
  claim_text TEXT NOT NULL,
  confidence DECIMAL(5,2) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'CANDIDATE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(claim_id),
  KEY idx_zentide_claim_change(change_id),
  CONSTRAINT fk_zentide_claim_change FOREIGN KEY(change_id) REFERENCES ow_zentide_change(change_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE ow_zentide_evidence
  ADD COLUMN source_id BIGINT UNSIGNED NULL,
  ADD COLUMN document_version_id BIGINT UNSIGNED NULL,
  ADD COLUMN claim_id BIGINT UNSIGNED NULL,
  ADD KEY idx_zentide_evidence_source(source_id),
  ADD KEY idx_zentide_evidence_document(document_version_id),
  ADD KEY idx_zentide_evidence_claim(claim_id),
  ADD CONSTRAINT fk_zentide_evidence_source FOREIGN KEY(source_id) REFERENCES ow_zentide_source(source_id),
  ADD CONSTRAINT fk_zentide_evidence_document FOREIGN KEY(document_version_id) REFERENCES ow_zentide_document_version(document_version_id),
  ADD CONSTRAINT fk_zentide_evidence_claim FOREIGN KEY(claim_id) REFERENCES ow_zentide_claim(claim_id);

CREATE TABLE IF NOT EXISTS ow_zentide_fact_audit (
  audit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  actor_type VARCHAR(24) NOT NULL,
  actor_id VARCHAR(100) NULL,
  action VARCHAR(60) NOT NULL,
  target_type VARCHAR(40) NOT NULL,
  target_id VARCHAR(100) NOT NULL,
  before_hash CHAR(64) NULL,
  after_hash CHAR(64) NULL,
  detail_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(audit_id),
  KEY idx_zentide_fact_audit_target(target_type,target_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-immutable-fact-lineage'
WHERE metadata_key='platform_phase';
