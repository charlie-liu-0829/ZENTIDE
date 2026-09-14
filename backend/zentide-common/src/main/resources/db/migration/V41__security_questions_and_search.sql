-- Account recovery and search integration metadata.
-- Answers are stored as BCrypt hashes; the question itself is safe to display
-- only after the account email has been supplied.
SET @has_security_question := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='security_question');
SET @sql := IF(@has_security_question=0, 'ALTER TABLE user_info ADD COLUMN security_question VARCHAR(200) NULL AFTER password', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_security_answer_hash := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='security_answer_hash');
SET @sql := IF(@has_security_answer_hash=0, 'ALTER TABLE user_info ADD COLUMN security_answer_hash VARCHAR(100) NULL AFTER security_question', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_security_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='user_info' AND index_name='idx_user_info_security_question');
SET @sql := IF(@has_security_index=0, 'CREATE INDEX idx_user_info_security_question ON user_info(email, security_question)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
