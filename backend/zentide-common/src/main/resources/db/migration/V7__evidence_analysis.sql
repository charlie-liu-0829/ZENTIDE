CREATE TABLE IF NOT EXISTS ow_agent_analysis (
  analysis_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,analysis_key BINARY(32) NOT NULL,user_id VARCHAR(10) NULL,
  change_event_id BIGINT UNSIGNED NULL,job_id BIGINT UNSIGNED NULL,analysis_type VARCHAR(32) NOT NULL,prompt_version VARCHAR(64) NOT NULL,
  model_provider VARCHAR(64) NOT NULL,model_name VARCHAR(128) NOT NULL,input_hash BINARY(32) NOT NULL,summary TEXT NULL,
  impact_level VARCHAR(16) NULL,recommendation TEXT NULL,matched_skills JSON NULL,skill_gaps JSON NULL,structured_output JSON NULL,
  confidence DECIMAL(5,4) NULL,validation_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',error_message VARCHAR(2000) NULL,
  input_tokens INT UNSIGNED NULL,output_tokens INT UNSIGNED NULL,estimated_cost DECIMAL(12,6) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(analysis_id),UNIQUE KEY uk_agent_analysis_key(analysis_key),KEY idx_agent_analysis_event_user(change_event_id,user_id),
  KEY idx_agent_analysis_job_created(job_id,created_at),KEY idx_agent_analysis_validation_created(validation_status,created_at),
  CONSTRAINT fk_agent_analysis_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE SET NULL,
  CONSTRAINT fk_agent_analysis_event FOREIGN KEY(change_event_id) REFERENCES ow_change_event(change_event_id) ON DELETE SET NULL,
  CONSTRAINT fk_agent_analysis_job FOREIGN KEY(job_id) REFERENCES ow_job(job_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_agent_analysis_evidence (
  analysis_evidence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,analysis_id BIGINT UNSIGNED NOT NULL,job_version_id BIGINT UNSIGNED NULL,
  snapshot_id BIGINT UNSIGNED NULL,evidence_role VARCHAR(32) NOT NULL,field_path VARCHAR(255) NULL,evidence_excerpt VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(analysis_evidence_id),
  UNIQUE KEY uk_analysis_evidence_version_snapshot_role(analysis_id,job_version_id,snapshot_id,evidence_role),
  KEY idx_analysis_evidence_version(job_version_id),KEY idx_analysis_evidence_snapshot(snapshot_id),
  CONSTRAINT fk_analysis_evidence_analysis FOREIGN KEY(analysis_id) REFERENCES ow_agent_analysis(analysis_id) ON DELETE CASCADE,
  CONSTRAINT fk_analysis_evidence_version FOREIGN KEY(job_version_id) REFERENCES ow_job_version(job_version_id) ON DELETE SET NULL,
  CONSTRAINT fk_analysis_evidence_snapshot FOREIGN KEY(snapshot_id) REFERENCES ow_page_snapshot(snapshot_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata(metadata_key,metadata_value) VALUES('platform_phase','evidence-analysis')
ON DUPLICATE KEY UPDATE metadata_value=VALUES(metadata_value);
