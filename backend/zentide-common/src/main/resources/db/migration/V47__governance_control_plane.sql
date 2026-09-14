CREATE TABLE IF NOT EXISTS zentide_governance_rule (
  rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  scene_id BIGINT UNSIGNED NOT NULL,
  violation_type VARCHAR(80) NOT NULL,
  keywords_json TEXT NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'medium',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(rule_id), KEY idx_governance_rule_scene(scene_id,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS zentide_governance_result (
  result_id VARCHAR(80) NOT NULL, content_id BIGINT NOT NULL, scene_id BIGINT NOT NULL,
  payload_json LONGTEXT NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(result_id), KEY idx_governance_result_scene(scene_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS zentide_moderation_feedback (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, content_id BIGINT NOT NULL, agent_result_id VARCHAR(80) NOT NULL,
  final_action VARCHAR(40) NOT NULL, accepted_agent_advice TINYINT(1) NOT NULL,
  corrected_violation_types TEXT NOT NULL, reviewer_reason VARCHAR(2000) NOT NULL,
  reviewer_id VARCHAR(64) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
