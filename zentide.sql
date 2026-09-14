-- ZENTIDE complete MySQL bootstrap snapshot, current through schema V49.
-- This file creates the final current schema for a fresh/local database.
-- Project schema changes are maintained in this single file; the guarded upgrade block
-- at the end reconciles an existing local database with the current snapshot.

CREATE DATABASE IF NOT EXISTS zentide
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE zentide;

-- Source: V2__source_application_approval.sql
CREATE TABLE IF NOT EXISTS user_info (
    user_id VARCHAR(10) NOT NULL,
    nick_name VARCHAR(20) NOT NULL,
    avatar VARCHAR(500) NULL,
    handle VARCHAR(30) NULL,
    bio VARCHAR(500) NULL,
    cover_url VARCHAR(2048) NULL,
    location VARCHAR(80) NULL,
    website_url VARCHAR(2048) NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(100) NOT NULL,
    security_question VARCHAR(200) NULL,
    security_answer_hash VARCHAR(100) NULL,
    search_history_json JSON NULL,
    sex TINYINT NULL DEFAULT 2,
    join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time DATETIME NULL,
    last_login_ip VARCHAR(45) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    api_key VARCHAR(255) NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_info_email (email),
    UNIQUE KEY uk_user_info_nick_name (nick_name),
    UNIQUE KEY uk_user_info_handle (handle),
    KEY idx_user_info_status_join_time (status, join_time),
    KEY idx_user_info_security_question (email, security_question)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V11__zentide_radar_and_fact_foundation.sql
CREATE TABLE IF NOT EXISTS zentide_topic (
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

CREATE TABLE IF NOT EXISTS zentide_radar (
  radar_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id VARCHAR(10) NOT NULL,
  name VARCHAR(120) NOT NULL,
  attention_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  notification_strategy VARCHAR(24) NOT NULL DEFAULT 'DAILY',
  change_types_json JSON NULL,
  source_preferences_json JSON NULL,
  ignore_rules_json JSON NULL,
  user_context VARCHAR(1000) NULL,
  visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  version INT UNSIGNED NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(radar_id),
  KEY idx_zentide_radar_owner_status(owner_id,status),
  CONSTRAINT fk_zentide_radar_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_change (
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
  document_version_id BIGINT UNSIGNED NULL,
  entity_id BIGINT UNSIGNED NULL,
  change_key VARCHAR(500) NULL,
  what_happened TEXT NULL,
  verification_status VARCHAR(24) NOT NULL DEFAULT 'UNVERIFIED',
  corrected_by_change_id BIGINT UNSIGNED NULL,
  supersedes_change_id BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(change_id),
  UNIQUE KEY uk_zentide_change_key(change_key),
  KEY idx_zentide_change_status_time(status,occurred_at),
  KEY idx_zentide_change_entity(entity_type,entity_key),
  KEY idx_zentide_change_verification(verification_status,created_at),
  KEY idx_zentide_change_document(document_version_id),
  KEY idx_zentide_change_entity_id(entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_evidence (
  evidence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  change_id BIGINT UNSIGNED NOT NULL,
  source_name VARCHAR(200) NOT NULL,
  source_url VARCHAR(2048) NOT NULL,
  locator VARCHAR(500) NULL,
  excerpt TEXT NULL,
  trust_tier VARCHAR(24) NOT NULL DEFAULT 'PRIMARY',
  excerpt_hash CHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  source_id BIGINT UNSIGNED NULL,
  document_version_id BIGINT UNSIGNED NULL,
  claim_id BIGINT UNSIGNED NULL,
  PRIMARY KEY(evidence_id),
  KEY idx_zentide_evidence_change(change_id),
  KEY idx_zentide_evidence_source(source_id),
  KEY idx_zentide_evidence_document(document_version_id),
  KEY idx_zentide_evidence_claim(claim_id),
  CONSTRAINT fk_zentide_evidence_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_insight (
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
  KEY idx_zentide_insight_change_status(change_id,status),
  CONSTRAINT fk_zentide_insight_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_insight_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V12__zentide_immutable_fact_lineage.sql
CREATE TABLE IF NOT EXISTS zentide_source (
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
CREATE TABLE IF NOT EXISTS zentide_fetch_attempt (
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
  CONSTRAINT fk_zentide_fetch_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_raw_snapshot (
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
  CONSTRAINT fk_zentide_snapshot_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id),
  CONSTRAINT fk_zentide_snapshot_attempt FOREIGN KEY(attempt_id) REFERENCES zentide_fetch_attempt(attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_document (
  document_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  canonical_key VARCHAR(500) NOT NULL,
  current_version_no INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(document_id),
  UNIQUE KEY uk_zentide_document_key(source_id,canonical_key),
  CONSTRAINT fk_zentide_document_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_document_version (
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
  CONSTRAINT fk_zentide_document_version_document FOREIGN KEY(document_id) REFERENCES zentide_document(document_id),
  CONSTRAINT fk_zentide_document_version_snapshot FOREIGN KEY(raw_snapshot_id) REFERENCES zentide_raw_snapshot(raw_snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_tracked_entity (
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
  CONSTRAINT fk_zentide_entity_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS zentide_claim (
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
  CONSTRAINT fk_zentide_claim_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- These constraints reference tables declared later than their owners. Guard
-- each name so this same snapshot can upgrade an existing database repeatedly.
DROP PROCEDURE IF EXISTS zentide_apply_core_foreign_keys;
DELIMITER $$
CREATE PROCEDURE zentide_apply_core_foreign_keys()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='fk_zentide_change_document_version') THEN
    ALTER TABLE zentide_change ADD CONSTRAINT fk_zentide_change_document_version FOREIGN KEY(document_version_id) REFERENCES zentide_document_version(document_version_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='fk_zentide_change_entity') THEN
    ALTER TABLE zentide_change ADD CONSTRAINT fk_zentide_change_entity FOREIGN KEY(entity_id) REFERENCES zentide_tracked_entity(entity_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='fk_zentide_change_correction') THEN
    ALTER TABLE zentide_change ADD CONSTRAINT fk_zentide_change_correction FOREIGN KEY(corrected_by_change_id) REFERENCES zentide_change(change_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='fk_zentide_change_supersedes') THEN
    ALTER TABLE zentide_change ADD CONSTRAINT fk_zentide_change_supersedes FOREIGN KEY(supersedes_change_id) REFERENCES zentide_change(change_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='fk_zentide_evidence_source') THEN
    ALTER TABLE zentide_evidence ADD CONSTRAINT fk_zentide_evidence_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='fk_zentide_evidence_document') THEN
    ALTER TABLE zentide_evidence ADD CONSTRAINT fk_zentide_evidence_document FOREIGN KEY(document_version_id) REFERENCES zentide_document_version(document_version_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_name='fk_zentide_evidence_claim') THEN
    ALTER TABLE zentide_evidence ADD CONSTRAINT fk_zentide_evidence_claim FOREIGN KEY(claim_id) REFERENCES zentide_claim(claim_id);
  END IF;
END$$
DELIMITER ;
CALL zentide_apply_core_foreign_keys();
DROP PROCEDURE zentide_apply_core_foreign_keys;

CREATE TABLE IF NOT EXISTS zentide_fact_audit (
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

-- Source: V13__zentide_retrieval_v1.sql
CREATE TABLE IF NOT EXISTS zentide_retrieval_chunk (
  chunk_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  document_version_id BIGINT UNSIGNED NOT NULL,
  chunk_no INT UNSIGNED NOT NULL,
  content_start INT UNSIGNED NOT NULL,
  content_end INT UNSIGNED NOT NULL,
  chunk_text TEXT NOT NULL,
  content_hash CHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  indexed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(chunk_id),
  UNIQUE KEY uk_zentide_retrieval_chunk_version_no(document_version_id,chunk_no),
  KEY idx_zentide_retrieval_chunk_status(status,document_version_id),
  CONSTRAINT fk_zentide_retrieval_chunk_version FOREIGN KEY(document_version_id) REFERENCES zentide_document_version(document_version_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V14__zentide_personal_insights_v1.sql
-- Insight feedback is stored in zentide_user_action as target_type='INSIGHT'.

-- Source: V15__zentide_daily_briefing.sql
CREATE TABLE IF NOT EXISTS zentide_briefing (
  briefing_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id VARCHAR(10) NOT NULL,
  briefing_date DATE NOT NULL,
  briefing_type VARCHAR(20) NOT NULL DEFAULT 'DAILY',
  headline VARCHAR(300) NOT NULL,
  body_json JSON NOT NULL,
  item_count INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(briefing_id),
  UNIQUE KEY uk_zentide_briefing_owner_date_type(owner_id,briefing_date,briefing_type),
  KEY idx_zentide_briefing_owner_time(owner_id,briefing_date DESC),
  CONSTRAINT fk_zentide_briefing_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V16__zentide_change_centered_community.sql
CREATE TABLE IF NOT EXISTS zentide_change_discussion (
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
  CONSTRAINT fk_zentide_discussion_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_discussion_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_discussion_parent FOREIGN KEY(parent_discussion_id) REFERENCES zentide_change_discussion(discussion_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_discussion_evidence FOREIGN KEY(evidence_id) REFERENCES zentide_evidence(evidence_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V17__zentide_community_evidence_review.sql
CREATE TABLE IF NOT EXISTS zentide_evidence_submission (
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
  CONSTRAINT fk_zentide_evidence_submission_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_evidence_submission_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_evidence_submission_evidence FOREIGN KEY(accepted_evidence_id) REFERENCES zentide_evidence(evidence_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V18__zentide_topic_community_foundation.sql
-- Topic/source and topic/change links live in zentide_topic_link; topic follows
-- live in zentide_user_action.

-- Source: V19__remove_legacy_recruitment_schema.sql
-- Legacy recruitment and notification tables are intentionally absent from this final snapshot.

-- Source: V20__zentide_change_watch.sql
-- Change watches live in zentide_user_action.

-- Source: V21__zentide_wave_stance_and_observation.sql
CREATE TABLE IF NOT EXISTS zentide_signal (
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
  CONSTRAINT fk_zentide_signal_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id),
  CONSTRAINT fk_zentide_signal_document_version FOREIGN KEY(document_version_id) REFERENCES zentide_document_version(document_version_id),
  CONSTRAINT fk_zentide_signal_change FOREIGN KEY(linked_change_id) REFERENCES zentide_change(change_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Signal/topic links live in zentide_topic_link; change stances live in
-- zentide_user_action.
CREATE TABLE IF NOT EXISTS zentide_observation_activity (
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
  CONSTRAINT fk_zentide_observation_activity_signal FOREIGN KEY(signal_id) REFERENCES zentide_signal(signal_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_observation_activity_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_observation_activity_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V22__zentide_radar_observation_plan.sql
UPDATE zentide_radar
SET change_types_json=JSON_ARRAY('RELEASE','BREAKING_CHANGE','SECURITY'),
    source_preferences_json=JSON_ARRAY('ALL'),
    ignore_rules_json=JSON_ARRAY()
WHERE change_types_json IS NULL;

-- Source: V23__zentide_source_application_admission.sql
CREATE TABLE IF NOT EXISTS zentide_source_application (
  application_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  topic_id BIGINT UNSIGNED NOT NULL,
  applicant_id VARCHAR(10) NOT NULL,
  source_name VARCHAR(200) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  canonical_url VARCHAR(2048) NOT NULL,
  canonical_url_hash CHAR(64) NOT NULL,
  trust_tier VARCHAR(24) NOT NULL DEFAULT 'COMMUNITY',
  status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED',
  review_note VARCHAR(1000) NULL,
  reviewer_account VARCHAR(100) NULL,
  source_id BIGINT UNSIGNED NULL,
  trial_result_json JSON NULL,
  trialed_at DATETIME(3) NULL,
  reviewed_at DATETIME(3) NULL,
  activated_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(application_id),
  KEY idx_zentide_source_application_queue(status,created_at),
  KEY idx_zentide_source_application_applicant(applicant_id,created_at),
  KEY idx_zentide_source_application_url(canonical_url_hash),
  CONSTRAINT fk_zentide_source_application_topic FOREIGN KEY(topic_id) REFERENCES zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_source_application_user FOREIGN KEY(applicant_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_source_application_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Application review history reuses zentide_fact_audit with
-- target_type='SOURCE_APPLICATION'.

-- Source: V25__zentide_transactional_outbox.sql
CREATE TABLE IF NOT EXISTS zentide_outbox_event (
  id VARCHAR(32) NOT NULL,
  event_id VARCHAR(32) NOT NULL,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  event_version INT NOT NULL,
  payload JSON NOT NULL,
  trace_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  published_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_zentide_outbox_event_id (event_id),
  UNIQUE KEY uk_zentide_outbox_aggregate_event (aggregate_type, aggregate_id, event_type, event_version),
  KEY idx_zentide_outbox_pending (status, next_retry_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_processed_event (
  consumer_name VARCHAR(128) NOT NULL,
  event_id VARCHAR(32) NOT NULL,
  processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (consumer_name, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V45__direct_messages.sql
CREATE TABLE IF NOT EXISTS zentide_direct_message (
  message_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sender_id VARCHAR(10) NOT NULL,
  recipient_id VARCHAR(10) NOT NULL,
  body VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SENT',
  read_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(message_id),
  KEY idx_zentide_dm_sender_peer(sender_id,recipient_id,created_at,message_id),
  KEY idx_zentide_dm_recipient_peer(recipient_id,sender_id,created_at,message_id),
  KEY idx_zentide_dm_unread(recipient_id,status,read_at,created_at),
  CONSTRAINT fk_zentide_dm_sender FOREIGN KEY(sender_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_dm_recipient FOREIGN KEY(recipient_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V26__zentide_interest_community.sql
-- Source: V46__hub_directions.sql
CREATE TABLE IF NOT EXISTS zentide_hub_direction (
  direction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  display_name VARCHAR(40) NOT NULL,
  description VARCHAR(160) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(direction_id),
  UNIQUE KEY uk_zentide_hub_direction_code(code),
  KEY idx_zentide_hub_direction_status_sort(status,sort_order,direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS zentide_hub (
  hub_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  slug VARCHAR(80) NOT NULL,
  owner_id VARCHAR(10) NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  category VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
  cover_url VARCHAR(2048) NULL,
  join_policy VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  join_question VARCHAR(200) NULL,
  join_answer VARCHAR(200) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
  member_count INT UNSIGNED NOT NULL DEFAULT 0,
  post_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(hub_id), UNIQUE KEY uk_zentide_hub_slug(slug),
  KEY idx_zentide_hub_status(status,member_count), KEY idx_zentide_hub_owner(owner_id,created_at),
  CONSTRAINT fk_zentide_hub_owner FOREIGN KEY(owner_id) REFERENCES user_info(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO zentide_hub_direction(code,display_name,description,status,sort_order,created_by) VALUES
 ('GENERAL','综合兴趣','跨领域讨论与未归类的兴趣现场','ACTIVE',10,'system'),
 ('TECHNOLOGY','科技','产品、工具、模型与技术趋势','ACTIVE',20,'system'),
 ('AI','AI','人工智能模型、工具与工作流','ACTIVE',25,'system'),
 ('MUSIC','音乐','演出、专辑、乐队与现场体验','ACTIVE',30,'system'),
 ('GAMES','游戏','游戏作品、版本、攻略与组队','ACTIVE',40,'system'),
 ('LIFESTYLE','生活方式','城市生活、消费与日常兴趣','ACTIVE',50,'system'),
 ('CULTURE','文化','影视、阅读、艺术与文化观察','ACTIVE',60,'system'),
 ('SPORTS','运动','赛事、训练与运动装备','ACTIVE',70,'system');
CREATE TABLE IF NOT EXISTS zentide_hub_category (
  category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(10) NOT NULL,
  name VARCHAR(40) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(category_id),
  UNIQUE KEY uk_zentide_hub_category_user_name(user_id,name),
  KEY idx_zentide_hub_category_user_sort(user_id,sort_order,category_id),
  CONSTRAINT fk_zentide_hub_category_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_hub_member (
  hub_id BIGINT UNSIGNED NOT NULL, user_id VARCHAR(10) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', permissions_json JSON NULL, membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  application_note VARCHAR(1000) NULL,
  category_id BIGINT UNSIGNED NULL, notification_mode VARCHAR(20) NOT NULL DEFAULT 'HIGHLIGHTS',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(hub_id,user_id), KEY idx_zentide_hub_member_user(user_id,membership_status,created_at),
  KEY idx_zentide_hub_member_category(user_id,category_id),
  CONSTRAINT fk_zentide_hub_member_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_hub_member_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_hub_member_category FOREIGN KEY(category_id) REFERENCES zentide_hub_category(category_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_hub_invitation (
  invitation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hub_id BIGINT UNSIGNED NOT NULL,
  created_by VARCHAR(10) NOT NULL,
  invite_token VARCHAR(64) NOT NULL,
  expires_at DATETIME(3) NULL,
  max_uses INT UNSIGNED NULL,
  use_count INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(invitation_id),
  UNIQUE KEY uk_zentide_hub_invitation_token(invite_token),
  KEY idx_zentide_hub_invitation_hub(hub_id,status,created_at),
  CONSTRAINT fk_zentide_hub_invitation_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_hub_invitation_creator FOREIGN KEY(created_by) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_post_type (
  post_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hub_id BIGINT UNSIGNED NULL,
  type_code VARCHAR(24) NOT NULL,
  display_name VARCHAR(20) NOT NULL,
  description VARCHAR(160) NULL,
  system_fixed TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(post_type_id),
  UNIQUE KEY uk_zentide_post_type_code(type_code),
  KEY idx_zentide_post_type_scope(hub_id,system_fixed,status,sort_order),
  CONSTRAINT fk_zentide_post_type_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_interest_entity (
  entity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hub_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(32) NOT NULL, name VARCHAR(180) NOT NULL, subtitle VARCHAR(300) NULL,
  metadata_json JSON NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(entity_id), KEY idx_zentide_entity_hub(hub_id,status),
  CONSTRAINT fk_zentide_entity_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_interest_event (
  event_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hub_id BIGINT UNSIGNED NOT NULL, entity_id BIGINT UNSIGNED NULL,
  title VARCHAR(220) NOT NULL, description TEXT NULL, starts_at DATETIME(3) NULL, ends_at DATETIME(3) NULL,
  venue VARCHAR(220) NULL, source_url VARCHAR(2048) NULL, status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(event_id), KEY idx_zentide_event_hub_time(hub_id,starts_at),
  CONSTRAINT fk_zentide_event_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_event_entity FOREIGN KEY(entity_id) REFERENCES zentide_interest_entity(entity_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_interest_post (
  post_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hub_id BIGINT UNSIGNED NOT NULL, author_id VARCHAR(10) NOT NULL,
  entity_id BIGINT UNSIGNED NULL, event_id BIGINT UNSIGNED NULL, change_id BIGINT UNSIGNED NULL, post_type VARCHAR(24) NOT NULL DEFAULT 'DISCUSSION',
  title VARCHAR(220) NOT NULL, body TEXT NOT NULL, media_json JSON NULL, cover_url VARCHAR(2048) NULL, status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  like_count INT UNSIGNED NOT NULL DEFAULT 0, comment_count INT UNSIGNED NOT NULL DEFAULT 0, bookmark_count INT UNSIGNED NOT NULL DEFAULT 0, view_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(post_id), KEY idx_zentide_post_feed(hub_id,status,created_at), KEY idx_zentide_post_author(author_id,created_at), KEY idx_zentide_post_change(change_id,created_at),
  CONSTRAINT fk_zentide_post_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_post_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_post_entity FOREIGN KEY(entity_id) REFERENCES zentide_interest_entity(entity_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_post_event FOREIGN KEY(event_id) REFERENCES zentide_interest_event(event_id) ON DELETE SET NULL,
  CONSTRAINT fk_zentide_post_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS zentide_interest_comment (
  comment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, post_id BIGINT UNSIGNED NOT NULL, author_id VARCHAR(10) NOT NULL,
  parent_comment_id BIGINT UNSIGNED NULL, body VARCHAR(4000) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED', like_count INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(comment_id), KEY idx_zentide_comment_post(post_id,status,created_at),
  CONSTRAINT fk_zentide_comment_post FOREIGN KEY(post_id) REFERENCES zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_comment_author FOREIGN KEY(author_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_comment_parent FOREIGN KEY(parent_comment_id) REFERENCES zentide_interest_comment(comment_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Six former topic association tables are represented by one typed link table.
-- Nullable target columns retain real foreign keys and cascade cleanup; exactly
-- one target column is populated for each row.
CREATE TABLE IF NOT EXISTS zentide_topic_link (
  topic_link_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  topic_id BIGINT UNSIGNED NOT NULL,
  radar_id BIGINT UNSIGNED NULL,
  source_id BIGINT UNSIGNED NULL,
  change_id BIGINT UNSIGNED NULL,
  signal_id BIGINT UNSIGNED NULL,
  hub_id BIGINT UNSIGNED NULL,
  post_id BIGINT UNSIGNED NULL,
  link_role VARCHAR(32) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  weight DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  include_descendants TINYINT(1) NOT NULL DEFAULT 1,
  featured TINYINT(1) NOT NULL DEFAULT 0,
  counter_value INT UNSIGNED NOT NULL DEFAULT 0,
  note VARCHAR(500) NULL,
  created_by VARCHAR(100) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(topic_link_id),
  UNIQUE KEY uk_zentide_topic_link_radar(topic_id,radar_id),
  UNIQUE KEY uk_zentide_topic_link_source(topic_id,source_id),
  UNIQUE KEY uk_zentide_topic_link_change(topic_id,change_id),
  UNIQUE KEY uk_zentide_topic_link_signal(topic_id,signal_id),
  UNIQUE KEY uk_zentide_topic_link_hub(topic_id,hub_id),
  UNIQUE KEY uk_zentide_topic_link_post(topic_id,post_id),
  KEY idx_zentide_topic_link_radar(radar_id,topic_id),
  KEY idx_zentide_topic_link_source(source_id,topic_id,status),
  KEY idx_zentide_topic_link_change(change_id,topic_id),
  KEY idx_zentide_topic_link_signal(signal_id,topic_id),
  KEY idx_zentide_topic_link_hub(hub_id,featured,counter_value),
  KEY idx_zentide_topic_link_post(post_id,topic_id),
  CONSTRAINT fk_zentide_topic_link_topic FOREIGN KEY(topic_id) REFERENCES zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_radar FOREIGN KEY(radar_id) REFERENCES zentide_radar(radar_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_source FOREIGN KEY(source_id) REFERENCES zentide_source(source_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_signal FOREIGN KEY(signal_id) REFERENCES zentide_signal(signal_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_hub FOREIGN KEY(hub_id) REFERENCES zentide_hub(hub_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_topic_link_post FOREIGN KEY(post_id) REFERENCES zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT chk_zentide_topic_link_one_target CHECK (
    (radar_id IS NOT NULL)+(source_id IS NOT NULL)+(change_id IS NOT NULL)+
    (signal_id IS NOT NULL)+(hub_id IS NOT NULL)+(post_id IS NOT NULL)=1
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nine former interaction/feedback tables are represented by one typed action
-- table. Independent action_type rows allow one user to like, bookmark and
-- classify the same post simultaneously without losing any state.
CREATE TABLE IF NOT EXISTS zentide_user_action (
  user_action_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(10) NOT NULL,
  topic_id BIGINT UNSIGNED NULL,
  change_id BIGINT UNSIGNED NULL,
  insight_id BIGINT UNSIGNED NULL,
  comment_id BIGINT UNSIGNED NULL,
  post_id BIGINT UNSIGNED NULL,
  target_user_id VARCHAR(10) NULL,
  event_id BIGINT UNSIGNED NULL,
  interest_entity_id BIGINT UNSIGNED NULL,
  action_type VARCHAR(24) NOT NULL,
  action_value VARCHAR(40) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  note VARCHAR(1000) NULL,
  metadata_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(user_action_id),
  UNIQUE KEY uk_zentide_user_action_topic(user_id,topic_id,action_type),
  UNIQUE KEY uk_zentide_user_action_change(user_id,change_id,action_type),
  UNIQUE KEY uk_zentide_user_action_insight(user_id,insight_id,action_type),
  UNIQUE KEY uk_zentide_user_action_comment(user_id,comment_id,action_type),
  UNIQUE KEY uk_zentide_user_action_post(user_id,post_id,action_type),
  UNIQUE KEY uk_zentide_user_action_user(user_id,target_user_id,action_type),
  UNIQUE KEY uk_zentide_user_action_event(user_id,event_id,action_type),
  UNIQUE KEY uk_zentide_user_action_entity(user_id,interest_entity_id,action_type),
  KEY idx_zentide_user_action_topic(topic_id,action_type,status),
  KEY idx_zentide_user_action_change(change_id,action_type,status),
  KEY idx_zentide_user_action_insight(insight_id,action_type,status),
  KEY idx_zentide_user_action_comment(comment_id,action_type,status),
  KEY idx_zentide_user_action_post(post_id,action_type,status),
  KEY idx_zentide_user_action_target_user(target_user_id,action_type,status),
  KEY idx_zentide_user_action_event(event_id,action_type,status),
  KEY idx_zentide_user_action_entity(interest_entity_id,action_type,status),
  KEY idx_zentide_user_action_actor(user_id,action_type,status,updated_at),
  CONSTRAINT fk_zentide_user_action_actor FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_topic FOREIGN KEY(topic_id) REFERENCES zentide_topic(topic_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_change FOREIGN KEY(change_id) REFERENCES zentide_change(change_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_insight FOREIGN KEY(insight_id) REFERENCES zentide_insight(insight_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_comment FOREIGN KEY(comment_id) REFERENCES zentide_interest_comment(comment_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_post FOREIGN KEY(post_id) REFERENCES zentide_interest_post(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_target_user FOREIGN KEY(target_user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_event FOREIGN KEY(event_id) REFERENCES zentide_interest_event(event_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_user_action_entity FOREIGN KEY(interest_entity_id) REFERENCES zentide_interest_entity(entity_id) ON DELETE CASCADE,
  CONSTRAINT chk_zentide_user_action_one_target CHECK (
    (topic_id IS NOT NULL)+(change_id IS NOT NULL)+(insight_id IS NOT NULL)+
    (comment_id IS NOT NULL)+(post_id IS NOT NULL)+(target_user_id IS NOT NULL)+
    (event_id IS NOT NULL)+(interest_entity_id IS NOT NULL)=1
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Source: V36__compact_active_community_schema.sql
-- Comment and post interactions live in zentide_user_action.

-- Source: V28__zentide_user_social_and_event_attendance.sql
-- User follows, event attendance and entity interactions live in
-- zentide_user_action.

-- Minimal seed data for a usable fresh community. All statements are idempotent;
-- Flyway migrations will safely skip or reconcile these records on application startup.
INSERT IGNORE INTO zentide_hub (slug, name, description, category) VALUES
 ('tech-new', '科技新品', '发布会、硬件、应用和下一件想买的东西', 'TECHNOLOGY'),
 ('live-music', '现场音乐', '演唱会、音乐节、歌手现场和同好见面', 'MUSIC'),
 ('games', '游戏现场', '新作、版本、攻略和一起玩的日常', 'GAMES'),
 ('ai-lab', 'AI 实验室', '模型、工具、工作流和真实使用体验', 'AI');

INSERT INTO zentide_interest_event (hub_id, title, description, starts_at, venue, status)
SELECT h.hub_id, '下一场值得期待的科技发布', '关注发布会、参数和第一手体验。', '2026-09-10 19:00:00', '线上发布会', 'UPCOMING'
FROM zentide_hub h WHERE h.slug = 'tech-new'
  AND NOT EXISTS (SELECT 1 FROM zentide_interest_event e WHERE e.title = '下一场值得期待的科技发布');

INSERT INTO zentide_interest_event (hub_id, title, description, starts_at, venue, status)
SELECT h.hub_id, '城市现场音乐周末', '分享演出信息、购票提醒和现场体验。', '2026-09-19 18:30:00', '城市音乐现场', 'UPCOMING'
FROM zentide_hub h WHERE h.slug = 'live-music'
  AND NOT EXISTS (SELECT 1 FROM zentide_interest_event e WHERE e.title = '城市现场音乐周末');

-- Source: V30__zentide_interest_entity_seed.sql
INSERT INTO zentide_interest_entity (hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'PRODUCT', '下一代旗舰手机', '关注发布会、真实体验与长期使用反馈。', JSON_OBJECT('kind','technology')
FROM zentide_hub h WHERE h.slug='tech-new'
  AND NOT EXISTS (SELECT 1 FROM zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='下一代旗舰手机');
INSERT INTO zentide_interest_entity (hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'TOUR', '城市现场音乐周末', '演出信息、购票提醒、歌单和现场体验。', JSON_OBJECT('kind','music')
FROM zentide_hub h WHERE h.slug='live-music'
  AND NOT EXISTS (SELECT 1 FROM zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='城市现场音乐周末');
INSERT INTO zentide_interest_entity (hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'GAME', '年度游戏新作', '版本消息、攻略、组队与真实游玩反馈。', JSON_OBJECT('kind','game')
FROM zentide_hub h WHERE h.slug='games'
  AND NOT EXISTS (SELECT 1 FROM zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='年度游戏新作');
INSERT INTO zentide_interest_entity (hub_id, entity_type, name, subtitle, metadata_json)
SELECT h.hub_id, 'MODEL', '新一代通用模型', '能力变化、工作流和真实使用体验。', JSON_OBJECT('kind','ai')
FROM zentide_hub h WHERE h.slug='ai-lab'
  AND NOT EXISTS (SELECT 1 FROM zentide_interest_entity e WHERE e.hub_id=h.hub_id AND e.name='新一代通用模型');
UPDATE zentide_interest_event event_item
JOIN zentide_hub h ON h.hub_id=event_item.hub_id
JOIN zentide_interest_entity entity_item ON entity_item.hub_id=h.hub_id
SET event_item.entity_id=entity_item.entity_id
WHERE event_item.entity_id IS NULL
  AND ((h.slug='tech-new' AND entity_item.name='下一代旗舰手机') OR (h.slug='live-music' AND entity_item.name='城市现场音乐周末'));

-- Source: V31__zentide_custom_interest_hubs.sql
-- Its owner/visibility columns, owner index and FK are folded into zentide_hub above.

-- Source: V32__zentide_community_topics.sql
-- Hub/topic and post/topic links live in zentide_topic_link.

INSERT INTO zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '抢票攻略', 'INTEREST', JSON_ARRAY('购票','开票') WHERE NOT EXISTS (SELECT 1 FROM zentide_topic WHERE canonical_name='抢票攻略');
INSERT INTO zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '真实体验', 'INTEREST', JSON_ARRAY('上手','使用感受') WHERE NOT EXISTS (SELECT 1 FROM zentide_topic WHERE canonical_name='真实体验');
INSERT INTO zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '版本变化', 'INTEREST', JSON_ARRAY('更新','补丁') WHERE NOT EXISTS (SELECT 1 FROM zentide_topic WHERE canonical_name='版本变化');
INSERT INTO zentide_topic(canonical_name, topic_type, aliases_json)
SELECT '工作流实践', 'INTEREST', JSON_ARRAY('效率','实战') WHERE NOT EXISTS (SELECT 1 FROM zentide_topic WHERE canonical_name='工作流实践');
INSERT IGNORE INTO zentide_topic_link(topic_id,hub_id,featured)
SELECT t.topic_id,h.hub_id,1 FROM zentide_hub h JOIN zentide_topic t
  ON (h.slug='live-music' AND t.canonical_name='抢票攻略')
  OR (h.slug='tech-new' AND t.canonical_name='真实体验')
  OR (h.slug='games' AND t.canonical_name='版本变化')
  OR (h.slug='ai-lab' AND t.canonical_name='工作流实践');

INSERT INTO zentide_post_type(hub_id,type_code,display_name,description,system_fixed,status,sort_order,created_by)
SELECT NULL,'DISCUSSION','讨论','交换观点、信息和开放式想法',1,'ACTIVE',10,'system'
WHERE NOT EXISTS (SELECT 1 FROM zentide_post_type WHERE type_code='DISCUSSION');
INSERT INTO zentide_post_type(hub_id,type_code,display_name,description,system_fixed,status,sort_order,created_by)
SELECT NULL,'QUESTION','提问','提出明确问题并向社区寻求回答',1,'ACTIVE',20,'system'
WHERE NOT EXISTS (SELECT 1 FROM zentide_post_type WHERE type_code='QUESTION');
INSERT INTO zentide_post_type(hub_id,type_code,display_name,description,system_fixed,status,sort_order,created_by)
SELECT NULL,'EXPERIENCE','经验','分享亲身经历、方法和实践过程',1,'ACTIVE',30,'system'
WHERE NOT EXISTS (SELECT 1 FROM zentide_post_type WHERE type_code='EXPERIENCE');
INSERT INTO zentide_post_type(hub_id,type_code,display_name,description,system_fixed,status,sort_order,created_by)
SELECT NULL,'REVIEW','评测','围绕作品、产品或活动给出完整评价',1,'ACTIVE',40,'system'
WHERE NOT EXISTS (SELECT 1 FROM zentide_post_type WHERE type_code='REVIEW');
INSERT INTO zentide_post_type(hub_id,type_code,display_name,description,system_fixed,status,sort_order,created_by)
SELECT NULL,'EVENT','活动','发起或分享与兴趣相关的线下线上活动',1,'ACTIVE',50,'system'
WHERE NOT EXISTS (SELECT 1 FROM zentide_post_type WHERE type_code='EVENT');
INSERT INTO zentide_post_type(hub_id,type_code,display_name,description,system_fixed,status,sort_order,created_by)
SELECT NULL,'POLL','投票','通过选项快速收集社区成员的看法',1,'ACTIVE',60,'system'
WHERE NOT EXISTS (SELECT 1 FROM zentide_post_type WHERE type_code='POLL');

-- Source: V33__zentide_change_community_bridge.sql
-- Source: V34__zentide_post_detail_media_comments.sql
-- Their final columns, indexes, FKs and comment-reaction table are folded into the community tables above.

-- Source: V35__zentide_custom_user_profiles.sql
-- Source: V36__compact_active_community_schema.sql
-- Profile fields are folded into user_info; active engagement fields are folded
-- into the two relationship tables defined above.

-- Source: V37__remove_legacy_ow_table_prefix.sql
-- Source: V38__zentide_hub_membership_and_personal_categories.sql
-- Upgrade block for an existing database. Fresh databases already contain these columns.
DROP PROCEDURE IF EXISTS zentide_apply_v38;
DELIMITER $$
CREATE PROCEDURE zentide_apply_v38()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub' AND column_name='join_policy') THEN
    ALTER TABLE zentide_hub ADD COLUMN join_policy VARCHAR(20) NOT NULL DEFAULT 'OPEN' AFTER cover_url;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub_member' AND column_name='membership_status') THEN
    ALTER TABLE zentide_hub_member ADD COLUMN membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER role;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub_member' AND column_name='category_id') THEN
    ALTER TABLE zentide_hub_member ADD COLUMN category_id BIGINT UNSIGNED NULL AFTER membership_status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub_member' AND column_name='updated_at') THEN
    ALTER TABLE zentide_hub_member ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER created_at;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='zentide_hub_member' AND index_name='idx_zentide_hub_member_category') THEN
    ALTER TABLE zentide_hub_member ADD KEY idx_zentide_hub_member_category(user_id,category_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='zentide_hub_member' AND constraint_name='fk_zentide_hub_member_category') THEN
    ALTER TABLE zentide_hub_member ADD CONSTRAINT fk_zentide_hub_member_category FOREIGN KEY(category_id) REFERENCES zentide_hub_category(category_id) ON DELETE SET NULL;
  END IF;
END$$
DELIMITER ;
CALL zentide_apply_v38();
DROP PROCEDURE zentide_apply_v38;

-- Source: V39__zentide_configurable_post_types.sql
-- The idempotent post-type table and default platform types are folded into the
-- community snapshot above, so this single file upgrades both fresh and existing databases.

-- Source: V40__compact_topic_links_and_user_actions.sql
-- Migrate existing installations before removing the sixteen superseded
-- relationship tables. Every insert is idempotent so an interrupted upgrade can
-- safely rerun this file.
DROP PROCEDURE IF EXISTS zentide_apply_v40;
DELIMITER $$
CREATE PROCEDURE zentide_apply_v40()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_radar_topic') THEN
    INSERT INTO zentide_topic_link(topic_id,radar_id,weight,include_descendants,note)
    SELECT topic_id,radar_id,weight,include_descendants,note FROM zentide_radar_topic
    ON DUPLICATE KEY UPDATE weight=VALUES(weight),include_descendants=VALUES(include_descendants),note=VALUES(note);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_topic_source') THEN
    INSERT INTO zentide_topic_link(topic_id,source_id,link_role,status,created_by,created_at,updated_at)
    SELECT topic_id,source_id,platform_key,observation_status,added_by,created_at,updated_at FROM zentide_topic_source
    ON DUPLICATE KEY UPDATE link_role=VALUES(link_role),status=VALUES(status),created_by=VALUES(created_by),updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_topic_change') THEN
    INSERT INTO zentide_topic_link(topic_id,change_id,link_role,created_at)
    SELECT topic_id,change_id,assigned_by,created_at FROM zentide_topic_change
    ON DUPLICATE KEY UPDATE link_role=VALUES(link_role);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_signal_topic') THEN
    INSERT INTO zentide_topic_link(topic_id,signal_id,link_role,created_at)
    SELECT topic_id,signal_id,assigned_by,created_at FROM zentide_signal_topic
    ON DUPLICATE KEY UPDATE link_role=VALUES(link_role);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_hub_topic') THEN
    INSERT INTO zentide_topic_link(topic_id,hub_id,featured,counter_value,created_at)
    SELECT topic_id,hub_id,featured,post_count,created_at FROM zentide_hub_topic
    ON DUPLICATE KEY UPDATE featured=VALUES(featured),counter_value=VALUES(counter_value);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_interest_post_topic') THEN
    INSERT INTO zentide_topic_link(topic_id,post_id,created_at)
    SELECT topic_id,post_id,created_at FROM zentide_interest_post_topic
    ON DUPLICATE KEY UPDATE created_at=LEAST(zentide_topic_link.created_at,VALUES(created_at));
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_topic_follow') THEN
    INSERT INTO zentide_user_action(user_id,topic_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,topic_id,'FOLLOW',notification_mode,created_at,created_at FROM zentide_topic_follow
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_change_watch') THEN
    INSERT INTO zentide_user_action(user_id,change_id,action_type,action_value,status,created_at,updated_at)
    SELECT user_id,change_id,'WATCH',watch_mode,status,created_at,updated_at FROM zentide_change_watch
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status=VALUES(status),updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_change_stance') THEN
    INSERT INTO zentide_user_action(user_id,change_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,change_id,'STANCE',stance_type,created_at,updated_at FROM zentide_change_stance
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_insight_feedback') THEN
    INSERT INTO zentide_user_action(user_id,insight_id,action_type,action_value,note,created_at,updated_at)
    SELECT owner_id,insight_id,'FEEDBACK',feedback_type,note,created_at,updated_at FROM zentide_insight_feedback
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),note=VALUES(note),status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_interest_comment_reaction') THEN
    INSERT INTO zentide_user_action(user_id,comment_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,comment_id,'REACTION',reaction,created_at,created_at FROM zentide_interest_comment_reaction
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_interest_post_engagement') THEN
    INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,post_id,'REACTION',reaction,created_at,updated_at FROM zentide_interest_post_engagement WHERE reaction IS NOT NULL
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
    INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,post_id,'ACTION',action,created_at,updated_at FROM zentide_interest_post_engagement WHERE action IS NOT NULL
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
    INSERT INTO zentide_user_action(user_id,post_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,post_id,'BOOKMARK','BOOKMARKED',created_at,updated_at FROM zentide_interest_post_engagement WHERE bookmarked=1
    ON DUPLICATE KEY UPDATE status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_user_follow') THEN
    INSERT INTO zentide_user_action(user_id,target_user_id,action_type,action_value,created_at,updated_at)
    SELECT follower_id,following_id,'FOLLOW','FOLLOWING',created_at,created_at FROM zentide_user_follow
    ON DUPLICATE KEY UPDATE status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_interest_event_attendance') THEN
    INSERT INTO zentide_user_action(user_id,event_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,event_id,'ATTENDANCE',attendance_status,created_at,updated_at FROM zentide_interest_event_attendance
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_interest_entity_engagement') THEN
    INSERT INTO zentide_user_action(user_id,interest_entity_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,entity_id,'FOLLOW',notification_mode,created_at,updated_at FROM zentide_interest_entity_engagement WHERE followed=1
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
    INSERT INTO zentide_user_action(user_id,interest_entity_id,action_type,action_value,created_at,updated_at)
    SELECT user_id,entity_id,'ACTION',action,created_at,updated_at FROM zentide_interest_entity_engagement WHERE action IS NOT NULL
    ON DUPLICATE KEY UPDATE action_value=VALUES(action_value),status='ACTIVE',updated_at=VALUES(updated_at);
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='zentide_source_application_review') THEN
    INSERT INTO zentide_fact_audit(actor_type,actor_id,action,target_type,target_id,detail_json,created_at)
    SELECT r.actor_type,r.actor_id,r.action,'SOURCE_APPLICATION',CAST(r.application_id AS CHAR),
           JSON_OBJECT('note',r.note,'detail',r.detail_json),r.created_at
    FROM zentide_source_application_review r
    WHERE NOT EXISTS (
      SELECT 1 FROM zentide_fact_audit a
      WHERE a.target_type='SOURCE_APPLICATION'
        AND a.target_id COLLATE utf8mb4_unicode_ci=CAST(r.application_id AS CHAR) COLLATE utf8mb4_unicode_ci
        AND a.action COLLATE utf8mb4_unicode_ci=r.action COLLATE utf8mb4_unicode_ci
        AND a.created_at=r.created_at
        AND COALESCE(a.actor_id,'') COLLATE utf8mb4_unicode_ci=COALESCE(r.actor_id,'') COLLATE utf8mb4_unicode_ci
    );
  END IF;
END$$
DELIMITER ;
CALL zentide_apply_v40();
DROP PROCEDURE zentide_apply_v40;

DROP TABLE IF EXISTS zentide_radar_topic;
DROP TABLE IF EXISTS zentide_topic_source;
DROP TABLE IF EXISTS zentide_topic_follow;
DROP TABLE IF EXISTS zentide_topic_change;
DROP TABLE IF EXISTS zentide_change_watch;
DROP TABLE IF EXISTS zentide_signal_topic;
DROP TABLE IF EXISTS zentide_change_stance;
DROP TABLE IF EXISTS zentide_insight_feedback;
DROP TABLE IF EXISTS zentide_source_application_review;
DROP TABLE IF EXISTS zentide_interest_comment_reaction;
DROP TABLE IF EXISTS zentide_interest_post_engagement;
DROP TABLE IF EXISTS zentide_user_follow;
DROP TABLE IF EXISTS zentide_interest_event_attendance;
DROP TABLE IF EXISTS zentide_interest_entity_engagement;
DROP TABLE IF EXISTS zentide_hub_topic;
DROP TABLE IF EXISTS zentide_interest_post_topic;

-- V41: password recovery metadata (safe to run against an existing database).
SET @has_security_question := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='security_question');
SET @sql := IF(@has_security_question=0, 'ALTER TABLE user_info ADD COLUMN security_question VARCHAR(200) NULL AFTER password', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_security_answer_hash := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='security_answer_hash');
SET @sql := IF(@has_security_answer_hash=0, 'ALTER TABLE user_info ADD COLUMN security_answer_hash VARCHAR(100) NULL AFTER security_question', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_security_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='user_info' AND index_name='idx_user_info_security_question');
SET @sql := IF(@has_security_index=0, 'CREATE INDEX idx_user_info_security_question ON user_info(email, security_question)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V42: compact per-user search history (latest five entries).
SET @has_search_history := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='search_history_json');
SET @sql := IF(@has_search_history=0, 'ALTER TABLE user_info ADD COLUMN search_history_json JSON NULL AFTER security_answer_hash', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V43: visible post view counter.
SET @has_post_view_count := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_interest_post' AND column_name='view_count');
SET @sql := IF(@has_post_view_count=0, 'ALTER TABLE zentide_interest_post ADD COLUMN view_count INT UNSIGNED NOT NULL DEFAULT 0 AFTER bookmark_count', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V44: per-interest-hub administrator permissions.
SET @has_hub_permissions := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub_member' AND column_name='permissions_json');
SET @sql := IF(@has_hub_permissions=0, 'ALTER TABLE zentide_hub_member ADD COLUMN permissions_json JSON NULL AFTER role', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE zentide_hub_member SET permissions_json='[]' WHERE role='ADMIN' AND permissions_json IS NULL;

-- V45 and V46 are represented by the idempotent CREATE TABLE statements above.
-- V48 repairs the hub admission columns for databases upgraded through older
-- incremental migrations; the final snapshot already contains those columns.
-- V47: Spring Boot-owned governance control plane. Python Agents never persist
-- governance business data directly; they call the internal admin callbacks.
CREATE TABLE IF NOT EXISTS zentide_governance_rule (
  rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, scene_id BIGINT UNSIGNED NOT NULL,
  violation_type VARCHAR(80) NOT NULL, keywords_json TEXT NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'medium', enabled TINYINT(1) NOT NULL DEFAULT 1,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(rule_id), KEY idx_governance_rule_scene(scene_id,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS zentide_governance_result (
  result_id VARCHAR(80) NOT NULL, content_id BIGINT NOT NULL, scene_id BIGINT NOT NULL,
  risk_level VARCHAR(16) NOT NULL DEFAULT 'low', suggested_action VARCHAR(40) NOT NULL DEFAULT 'allow',
  confidence DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
  payload_json LONGTEXT NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(result_id), KEY idx_governance_result_scene(scene_id,created_at),
  KEY idx_governance_result_risk(scene_id,risk_level,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS zentide_moderation_feedback (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, content_id BIGINT NOT NULL, agent_result_id VARCHAR(80) NOT NULL,
  final_action VARCHAR(40) NOT NULL, accepted_agent_advice TINYINT(1) NOT NULL,
  corrected_violation_types TEXT NOT NULL, reviewer_reason VARCHAR(2000) NOT NULL,
  reviewer_id VARCHAR(64) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  KEY idx_moderation_feedback_result(agent_result_id), KEY idx_moderation_feedback_reviewer(reviewer_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Current snapshot version: V49 (39 tables; governance tables are intentionally separate).
