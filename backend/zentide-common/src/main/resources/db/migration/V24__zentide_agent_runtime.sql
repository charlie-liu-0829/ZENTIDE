CREATE TABLE IF NOT EXISTS ow_zentide_agent_run (
  run_id VARCHAR(32) NOT NULL,
  user_id VARCHAR(32) NULL,
  run_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  model_policy VARCHAR(64) NOT NULL,
  max_steps INT NOT NULL,
  max_tool_calls INT NOT NULL,
  max_tokens BIGINT NOT NULL,
  max_cost DECIMAL(10,4) NOT NULL,
  used_steps INT NOT NULL DEFAULT 0,
  used_tool_calls INT NOT NULL DEFAULT 0,
  used_tokens BIGINT NOT NULL DEFAULT 0,
  used_cost DECIMAL(10,4) NOT NULL DEFAULT 0,
  next_resume_at DATETIME(3) NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  stop_reason VARCHAR(128) NULL,
  trace_id VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (run_id),
  KEY idx_agent_run_status_resume (status, next_resume_at),
  KEY idx_agent_run_user (user_id, created_at),
  KEY idx_agent_run_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_agent_step (
  step_id VARCHAR(32) NOT NULL,
  run_id VARCHAR(32) NOT NULL,
  step_no INT NOT NULL,
  step_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  input_hash CHAR(64) NULL,
  output_summary TEXT NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (step_id),
  UNIQUE KEY uk_agent_step (run_id, step_no),
  CONSTRAINT fk_agent_step_run FOREIGN KEY (run_id) REFERENCES ow_zentide_agent_run(run_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_agent_tool_call (
  tool_call_id VARCHAR(32) NOT NULL,
  run_id VARCHAR(32) NOT NULL,
  step_id VARCHAR(32) NOT NULL,
  tool_name VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_json JSON NOT NULL,
  response_json JSON NULL,
  evidence_ids JSON NULL,
  status VARCHAR(32) NOT NULL,
  retryable BOOLEAN NOT NULL DEFAULT FALSE,
  duration_ms INT NULL,
  error_code VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  finished_at DATETIME(3) NULL,
  PRIMARY KEY (tool_call_id),
  UNIQUE KEY uk_tool_call_idempotency (run_id, idempotency_key),
  KEY idx_tool_call_run (run_id, created_at),
  CONSTRAINT fk_tool_call_run FOREIGN KEY (run_id) REFERENCES ow_zentide_agent_run(run_id) ON DELETE CASCADE,
  CONSTRAINT fk_tool_call_step FOREIGN KEY (step_id) REFERENCES ow_zentide_agent_step(step_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_agent_checkpoint (
  checkpoint_id VARCHAR(32) NOT NULL,
  run_id VARCHAR(32) NOT NULL,
  step_no INT NOT NULL,
  state_json JSON NOT NULL,
  state_hash CHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (checkpoint_id),
  UNIQUE KEY uk_agent_checkpoint_step (run_id, step_no),
  CONSTRAINT fk_checkpoint_run FOREIGN KEY (run_id) REFERENCES ow_zentide_agent_run(run_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ow_zentide_human_review (
  review_id VARCHAR(32) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id VARCHAR(64) NOT NULL,
  review_type VARCHAR(64) NOT NULL,
  priority VARCHAR(16) NOT NULL,
  reason VARCHAR(1024) NOT NULL,
  status VARCHAR(32) NOT NULL,
  assigned_to VARCHAR(32) NULL,
  decision VARCHAR(32) NULL,
  decision_note TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  resolved_at DATETIME(3) NULL,
  PRIMARY KEY (review_id),
  KEY idx_review_queue (status, priority, created_at),
  KEY idx_review_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
