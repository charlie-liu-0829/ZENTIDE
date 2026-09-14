-- V49: enforce the post editor contract and make governance work-queue queries cheap.
-- The title change intentionally fails if legacy rows still contain NULL titles;
-- operators must clean those rows explicitly instead of silently inventing content.
ALTER TABLE zentide_interest_post
  MODIFY COLUMN title VARCHAR(220) NOT NULL;

SET @zentide_add_risk_level := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_governance_result' AND column_name='risk_level')=0,
  'ALTER TABLE zentide_governance_result ADD COLUMN risk_level VARCHAR(16) NOT NULL DEFAULT ''low'' AFTER scene_id',
  'SELECT 1'
);
PREPARE zentide_v49_stmt FROM @zentide_add_risk_level; EXECUTE zentide_v49_stmt; DEALLOCATE PREPARE zentide_v49_stmt;
SET @zentide_add_suggested_action := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_governance_result' AND column_name='suggested_action')=0,
  'ALTER TABLE zentide_governance_result ADD COLUMN suggested_action VARCHAR(40) NOT NULL DEFAULT ''allow'' AFTER risk_level',
  'SELECT 1'
);
PREPARE zentide_v49_stmt FROM @zentide_add_suggested_action; EXECUTE zentide_v49_stmt; DEALLOCATE PREPARE zentide_v49_stmt;
SET @zentide_add_confidence := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_governance_result' AND column_name='confidence')=0,
  'ALTER TABLE zentide_governance_result ADD COLUMN confidence DECIMAL(5,4) NOT NULL DEFAULT 0.0000 AFTER suggested_action',
  'SELECT 1'
);
PREPARE zentide_v49_stmt FROM @zentide_add_confidence; EXECUTE zentide_v49_stmt; DEALLOCATE PREPARE zentide_v49_stmt;

SET @zentide_add_risk_index := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='zentide_governance_result' AND index_name='idx_governance_result_risk')=0,
  'ALTER TABLE zentide_governance_result ADD KEY idx_governance_result_risk(scene_id,risk_level,created_at)',
  'SELECT 1'
);
PREPARE zentide_v49_stmt FROM @zentide_add_risk_index; EXECUTE zentide_v49_stmt; DEALLOCATE PREPARE zentide_v49_stmt;
SET @zentide_add_feedback_result_index := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='zentide_moderation_feedback' AND index_name='idx_moderation_feedback_result')=0,
  'ALTER TABLE zentide_moderation_feedback ADD KEY idx_moderation_feedback_result(agent_result_id)',
  'SELECT 1'
);
PREPARE zentide_v49_stmt FROM @zentide_add_feedback_result_index; EXECUTE zentide_v49_stmt; DEALLOCATE PREPARE zentide_v49_stmt;
SET @zentide_add_feedback_reviewer_index := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='zentide_moderation_feedback' AND index_name='idx_moderation_feedback_reviewer')=0,
  'ALTER TABLE zentide_moderation_feedback ADD KEY idx_moderation_feedback_reviewer(reviewer_id,created_at)',
  'SELECT 1'
);
PREPARE zentide_v49_stmt FROM @zentide_add_feedback_reviewer_index; EXECUTE zentide_v49_stmt; DEALLOCATE PREPARE zentide_v49_stmt;
