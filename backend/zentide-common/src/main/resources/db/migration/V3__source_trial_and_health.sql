CREATE TABLE IF NOT EXISTS ow_parser_configuration (
    parser_config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_id BIGINT UNSIGNED NOT NULL,
    config_version INT NOT NULL,
    adapter_code VARCHAR(100) NOT NULL,
    parser_type VARCHAR(32) NOT NULL,
    configuration JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    activated_at DATETIME(3) NULL,
    PRIMARY KEY (parser_config_id),
    UNIQUE KEY uk_parser_source_version (source_id, config_version),
    KEY idx_parser_source_status (source_id, status),
    CONSTRAINT fk_parser_source FOREIGN KEY (source_id)
        REFERENCES ow_recruitment_source (source_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_crawl_task (
    task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_id BIGINT UNSIGNED NOT NULL,
    task_key VARCHAR(128) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority TINYINT NOT NULL DEFAULT 5,
    scheduled_at DATETIME(3) NOT NULL,
    started_at DATETIME(3) NULL,
    finished_at DATETIME(3) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 0,
    locked_by VARCHAR(128) NULL,
    locked_at DATETIME(3) NULL,
    last_error_code VARCHAR(64) NULL,
    last_error_message VARCHAR(2000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (task_id),
    UNIQUE KEY uk_crawl_task_key (task_key),
    KEY idx_crawl_task_dispatch (status, priority, scheduled_at),
    KEY idx_crawl_task_source_created (source_id, created_at),
    CONSTRAINT fk_crawl_task_source FOREIGN KEY (source_id)
        REFERENCES ow_recruitment_source (source_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_crawl_attempt (
    attempt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    task_id BIGINT UNSIGNED NOT NULL,
    source_id BIGINT UNSIGNED NOT NULL,
    attempt_no INT NOT NULL,
    status VARCHAR(24) NOT NULL,
    http_status SMALLINT UNSIGNED NULL,
    response_headers JSON NULL,
    etag VARCHAR(512) NULL,
    last_modified VARCHAR(255) NULL,
    content_hash BINARY(32) NULL,
    response_bytes BIGINT UNSIGNED NULL,
    redirect_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    jobs_parsed INT UNSIGNED NULL,
    error_stage VARCHAR(32) NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(2000) NULL,
    started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at DATETIME(3) NULL,
    PRIMARY KEY (attempt_id),
    UNIQUE KEY uk_crawl_attempt_task_no (task_id, attempt_no),
    KEY idx_crawl_attempt_source_started (source_id, started_at),
    KEY idx_crawl_attempt_status_started (status, started_at),
    CONSTRAINT fk_crawl_attempt_task FOREIGN KEY (task_id)
        REFERENCES ow_crawl_task (task_id) ON DELETE CASCADE,
    CONSTRAINT fk_crawl_attempt_source FOREIGN KEY (source_id)
        REFERENCES ow_recruitment_source (source_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_page_snapshot (
    snapshot_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_id BIGINT UNSIGNED NOT NULL,
    attempt_id BIGINT UNSIGNED NOT NULL,
    content_hash BINARY(32) NOT NULL,
    storage_type VARCHAR(20) NOT NULL DEFAULT 'DATABASE',
    object_key VARCHAR(1000) NULL,
    content_encoding VARCHAR(50) NULL,
    raw_content MEDIUMBLOB NULL,
    text_content MEDIUMTEXT NULL,
    captured_url VARCHAR(2048) NOT NULL,
    captured_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at DATETIME(3) NULL,
    PRIMARY KEY (snapshot_id),
    UNIQUE KEY uk_snapshot_source_hash (source_id, content_hash),
    KEY idx_snapshot_source_captured (source_id, captured_at),
    KEY idx_snapshot_expiration (expires_at),
    CONSTRAINT fk_snapshot_source FOREIGN KEY (source_id)
        REFERENCES ow_recruitment_source (source_id) ON DELETE CASCADE,
    CONSTRAINT fk_snapshot_attempt FOREIGN KEY (attempt_id)
        REFERENCES ow_crawl_attempt (attempt_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_parser_run_record (
    parser_run_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT UNSIGNED NOT NULL,
    snapshot_id BIGINT UNSIGNED NULL,
    parser_config_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL,
    jobs_found INT UNSIGNED NOT NULL DEFAULT 0,
    warning_count INT UNSIGNED NOT NULL DEFAULT 0,
    quality_score DECIMAL(5,4) NULL,
    warnings JSON NULL,
    error_message VARCHAR(2000) NULL,
    started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at DATETIME(3) NULL,
    PRIMARY KEY (parser_run_id),
    KEY idx_parser_run_attempt (attempt_id),
    KEY idx_parser_run_config_status (parser_config_id, status),
    CONSTRAINT fk_parser_run_attempt FOREIGN KEY (attempt_id)
        REFERENCES ow_crawl_attempt (attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_parser_run_snapshot FOREIGN KEY (snapshot_id)
        REFERENCES ow_page_snapshot (snapshot_id) ON DELETE SET NULL,
    CONSTRAINT fk_parser_run_config FOREIGN KEY (parser_config_id)
        REFERENCES ow_parser_configuration (parser_config_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_source_health_record (
    health_record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_id BIGINT UNSIGNED NOT NULL,
    attempt_id BIGINT UNSIGNED NULL,
    health_status VARCHAR(20) NOT NULL,
    fetch_success TINYINT(1) NOT NULL DEFAULT 0,
    parse_success TINYINT(1) NOT NULL DEFAULT 0,
    parse_success_rate_30d DECIMAL(5,4) NULL,
    jobs_found INT UNSIGNED NULL,
    consecutive_failures INT UNSIGNED NOT NULL DEFAULT 0,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(2000) NULL,
    recorded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (health_record_id),
    KEY idx_source_health_source_recorded (source_id, recorded_at),
    KEY idx_source_health_status_recorded (health_status, recorded_at),
    CONSTRAINT fk_source_health_source FOREIGN KEY (source_id)
        REFERENCES ow_recruitment_source (source_id) ON DELETE CASCADE,
    CONSTRAINT fk_source_health_attempt FOREIGN KEY (attempt_id)
        REFERENCES ow_crawl_attempt (attempt_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata (metadata_key, metadata_value)
VALUES ('platform_phase', 'source-trial')
ON DUPLICATE KEY UPDATE metadata_value = VALUES(metadata_value);
