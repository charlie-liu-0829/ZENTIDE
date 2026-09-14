-- 修复早期增量迁移遗漏的现场申请字段，使旧库迁移结果与 zentide.sql 快照一致。
SET @zentide_add_join_question := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub' AND column_name='join_question')=0, 'ALTER TABLE zentide_hub ADD COLUMN join_question VARCHAR(200) NULL AFTER join_policy', 'SELECT 1');
PREPARE stmt FROM @zentide_add_join_question; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @zentide_add_join_answer := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub' AND column_name='join_answer')=0, 'ALTER TABLE zentide_hub ADD COLUMN join_answer VARCHAR(200) NULL AFTER join_question', 'SELECT 1');
PREPARE stmt FROM @zentide_add_join_answer; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @zentide_add_application_note := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='zentide_hub_member' AND column_name='application_note')=0, 'ALTER TABLE zentide_hub_member ADD COLUMN application_note VARCHAR(1000) NULL AFTER membership_status', 'SELECT 1');
PREPARE stmt FROM @zentide_add_application_note; EXECUTE stmt; DEALLOCATE PREPARE stmt;
