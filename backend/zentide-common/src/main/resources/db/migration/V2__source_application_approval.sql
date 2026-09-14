CREATE TABLE IF NOT EXISTS user_info (
    user_id VARCHAR(10) NOT NULL,
    nick_name VARCHAR(20) NOT NULL,
    avatar VARCHAR(500) NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(100) NOT NULL,
    sex TINYINT NULL DEFAULT 2,
    join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time DATETIME NULL,
    last_login_ip VARCHAR(45) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    api_key VARCHAR(255) NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_info_email (email),
    UNIQUE KEY uk_user_info_nick_name (nick_name),
    KEY idx_user_info_status_join_time (status, join_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_company (
    company_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100) NULL,
    normalized_name VARCHAR(200) NOT NULL,
    official_site VARCHAR(2048) NULL,
    logo_url VARCHAR(2048) NULL,
    industry VARCHAR(100) NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    merged_into_company_id BIGINT UNSIGNED NULL,
    created_by VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (company_id),
    UNIQUE KEY uk_company_normalized_name (normalized_name),
    KEY idx_company_status_name (status, name),
    CONSTRAINT fk_company_merged_into FOREIGN KEY (merged_into_company_id)
        REFERENCES ow_company (company_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_company_alias (
    alias_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    company_id BIGINT UNSIGNED NOT NULL,
    alias_name VARCHAR(200) NOT NULL,
    normalized_alias VARCHAR(200) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (alias_id),
    UNIQUE KEY uk_company_alias_normalized (normalized_alias),
    KEY idx_company_alias_company (company_id),
    CONSTRAINT fk_company_alias_company FOREIGN KEY (company_id)
        REFERENCES ow_company (company_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_source_application (
    application_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    applicant_user_id VARCHAR(10) NULL,
    company_name VARCHAR(200) NOT NULL,
    official_site VARCHAR(2048) NULL,
    source_url VARCHAR(2048) NOT NULL,
    normalized_url VARCHAR(2048) NOT NULL,
    url_hash BINARY(32) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_CHECK',
    check_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    check_summary JSON NULL,
    rejection_code VARCHAR(64) NULL,
    rejection_reason VARCHAR(1000) NULL,
    reviewed_by VARCHAR(64) NULL,
    reviewed_at DATETIME(3) NULL,
    withdrawn_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    lock_version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (application_id),
    KEY idx_source_application_url_hash (url_hash),
    KEY idx_source_application_status_created (status, created_at),
    KEY idx_source_application_applicant (applicant_user_id, created_at),
    CONSTRAINT fk_source_application_user FOREIGN KEY (applicant_user_id)
        REFERENCES user_info (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_recruitment_source (
    source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    company_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NULL,
    source_url VARCHAR(2048) NOT NULL,
    normalized_url VARCHAR(2048) NOT NULL,
    url_hash BINARY(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'HTML_STATIC',
    status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    policy JSON NULL,
    robots_status VARCHAR(20) NULL,
    terms_url VARCHAR(2048) NULL,
    crawl_interval_minutes INT NOT NULL DEFAULT 360,
    domain_concurrency_limit INT NOT NULL DEFAULT 1,
    request_timeout_seconds INT NOT NULL DEFAULT 15,
    max_response_bytes BIGINT UNSIGNED NOT NULL DEFAULT 5242880,
    next_crawl_at DATETIME(3) NULL,
    last_attempt_at DATETIME(3) NULL,
    last_success_at DATETIME(3) NULL,
    consecutive_failures INT NOT NULL DEFAULT 0,
    activated_at DATETIME(3) NULL,
    paused_at DATETIME(3) NULL,
    pause_reason VARCHAR(1000) NULL,
    created_by VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    lock_version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (source_id),
    UNIQUE KEY uk_recruitment_source_url_hash (url_hash),
    KEY idx_recruitment_source_company_status (company_id, status),
    KEY idx_recruitment_source_next_crawl (status, next_crawl_at),
    CONSTRAINT fk_recruitment_source_company FOREIGN KEY (company_id)
        REFERENCES ow_company (company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_recruitment_source_application FOREIGN KEY (application_id)
        REFERENCES ow_source_application (application_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_source_review_record (
    review_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id BIGINT UNSIGNED NOT NULL,
    source_id BIGINT UNSIGNED NULL,
    reviewer_account VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    note VARCHAR(2000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (review_id),
    KEY idx_source_review_application_created (application_id, created_at),
    KEY idx_source_review_source_created (source_id, created_at),
    CONSTRAINT fk_source_review_application FOREIGN KEY (application_id)
        REFERENCES ow_source_application (application_id) ON DELETE CASCADE,
    CONSTRAINT fk_source_review_source FOREIGN KEY (source_id)
        REFERENCES ow_recruitment_source (source_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_url_security_check (
    check_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id BIGINT UNSIGNED NULL,
    source_id BIGINT UNSIGNED NULL,
    check_type VARCHAR(32) NOT NULL DEFAULT 'SUBMISSION',
    requested_url VARCHAR(2048) NOT NULL,
    final_url VARCHAR(2048) NULL,
    protocol VARCHAR(10) NULL,
    resolved_ips JSON NULL,
    redirect_chain JSON NULL,
    http_status SMALLINT UNSIGNED NULL,
    content_type VARCHAR(255) NULL,
    content_length BIGINT UNSIGNED NULL,
    robots_status VARCHAR(20) NULL,
    risk_code VARCHAR(64) NULL,
    risk_detail VARCHAR(2000) NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at DATETIME(3) NULL,
    PRIMARY KEY (check_id),
    KEY idx_url_security_application (application_id, started_at),
    KEY idx_url_security_source (source_id, started_at),
    KEY idx_url_security_status (status, started_at),
    CONSTRAINT fk_url_security_application FOREIGN KEY (application_id)
        REFERENCES ow_source_application (application_id) ON DELETE CASCADE,
    CONSTRAINT fk_url_security_source FOREIGN KEY (source_id)
        REFERENCES ow_recruitment_source (source_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_audit_log (
    audit_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(64) NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id VARCHAR(128) NULL,
    request_id VARCHAR(128) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(1000) NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message VARCHAR(2000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (audit_log_id),
    KEY idx_audit_actor_created (actor_type, actor_id, created_at),
    KEY idx_audit_target_created (target_type, target_id, created_at),
    KEY idx_audit_request_id (request_id),
    KEY idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata (metadata_key, metadata_value)
VALUES ('platform_phase', 'source-approval')
ON DUPLICATE KEY UPDATE metadata_value = VALUES(metadata_value);
