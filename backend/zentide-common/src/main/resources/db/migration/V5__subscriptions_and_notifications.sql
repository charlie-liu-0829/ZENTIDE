CREATE TABLE IF NOT EXISTS ow_subscription (
  subscription_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id VARCHAR(10) NOT NULL,company_id BIGINT UNSIGNED NOT NULL,
  source_id BIGINT UNSIGNED NULL,scope_key VARCHAR(128) NOT NULL DEFAULT 'COMPANY',focus_level VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  rule_config JSON NULL,notification_frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),cancelled_at DATETIME(3) NULL,
  PRIMARY KEY(subscription_id),UNIQUE KEY uk_subscription_user_company_scope(user_id,company_id,scope_key),
  KEY idx_subscription_company_status(company_id,status),KEY idx_subscription_user_status(user_id,status),
  CONSTRAINT fk_subscription_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_subscription_company FOREIGN KEY(company_id) REFERENCES ow_company(company_id) ON DELETE CASCADE,
  CONSTRAINT fk_subscription_source FOREIGN KEY(source_id) REFERENCES ow_recruitment_source(source_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_user_event_match (
  match_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id VARCHAR(10) NOT NULL,subscription_id BIGINT UNSIGNED NOT NULL,change_event_id BIGINT UNSIGNED NOT NULL,
  match_score DECIMAL(5,2) NOT NULL,priority VARCHAR(16) NOT NULL,rule_result JSON NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'MATCHED',matched_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(match_id),UNIQUE KEY uk_user_event_match(user_id,change_event_id),KEY idx_user_match_priority_time(user_id,priority,matched_at),KEY idx_event_match_priority(change_event_id,priority),
  CONSTRAINT fk_user_event_match_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_user_event_match_subscription FOREIGN KEY(subscription_id) REFERENCES ow_subscription(subscription_id) ON DELETE CASCADE,
  CONSTRAINT fk_user_event_match_event FOREIGN KEY(change_event_id) REFERENCES ow_change_event(change_event_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_notification (
  notification_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id VARCHAR(10) NOT NULL,change_event_id BIGINT UNSIGNED NULL,match_id BIGINT UNSIGNED NULL,analysis_id BIGINT UNSIGNED NULL,
  channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',template_code VARCHAR(64) NOT NULL,title VARCHAR(300) NOT NULL,content TEXT NOT NULL,
  aggregation_key VARCHAR(128) NULL,dedupe_key VARCHAR(128) NOT NULL,status VARCHAR(24) NOT NULL DEFAULT 'PENDING',retry_count INT UNSIGNED NOT NULL DEFAULT 0,
  scheduled_at DATETIME(3) NULL,sent_at DATETIME(3) NULL,read_at DATETIME(3) NULL,failure_code VARCHAR(64) NULL,failure_message VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(notification_id),UNIQUE KEY uk_notification_dedupe_key(dedupe_key),KEY idx_notification_user_status_created(user_id,status,created_at),KEY idx_notification_dispatch(status,scheduled_at,priority),
  CONSTRAINT fk_notification_user FOREIGN KEY(user_id) REFERENCES user_info(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_notification_event FOREIGN KEY(change_event_id) REFERENCES ow_change_event(change_event_id) ON DELETE SET NULL,
  CONSTRAINT fk_notification_match FOREIGN KEY(match_id) REFERENCES ow_user_event_match(match_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata(metadata_key,metadata_value) VALUES('platform_phase','subscriptions-and-notifications')
ON DUPLICATE KEY UPDATE metadata_value=VALUES(metadata_value);
