CREATE TABLE IF NOT EXISTS ow_zentide_outbox_event (
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

CREATE TABLE IF NOT EXISTS ow_zentide_processed_event (
  consumer_name VARCHAR(128) NOT NULL,
  event_id VARCHAR(32) NOT NULL,
  processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (consumer_name, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
