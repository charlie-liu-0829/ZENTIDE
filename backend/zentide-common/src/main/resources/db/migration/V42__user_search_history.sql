-- Keep the five latest search entries compactly on the user account.
SET @has_search_history := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_info' AND column_name='search_history_json');
SET @sql := IF(@has_search_history=0, 'ALTER TABLE user_info ADD COLUMN search_history_json JSON NULL AFTER security_answer_hash', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
