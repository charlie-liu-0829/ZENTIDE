SET @zentide_owner_column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ow_zentide_hub' AND column_name='owner_id');
SET @zentide_owner_sql := IF(@zentide_owner_column_exists=0, 'ALTER TABLE ow_zentide_hub ADD COLUMN owner_id VARCHAR(10) NULL AFTER slug', 'SELECT 1');
PREPARE zentide_owner_stmt FROM @zentide_owner_sql;
EXECUTE zentide_owner_stmt;
DEALLOCATE PREPARE zentide_owner_stmt;

SET @zentide_visibility_column_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ow_zentide_hub' AND column_name='visibility');
SET @zentide_visibility_sql := IF(@zentide_visibility_column_exists=0, 'ALTER TABLE ow_zentide_hub ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT ''PUBLIC'' AFTER status', 'SELECT 1');
PREPARE zentide_visibility_stmt FROM @zentide_visibility_sql;
EXECUTE zentide_visibility_stmt;
DEALLOCATE PREPARE zentide_visibility_stmt;

SET @zentide_owner_key_exists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='ow_zentide_hub' AND index_name='idx_zentide_hub_owner');
SET @zentide_owner_key_sql := IF(@zentide_owner_key_exists=0, 'ALTER TABLE ow_zentide_hub ADD KEY idx_zentide_hub_owner (owner_id, created_at)', 'SELECT 1');
PREPARE zentide_owner_key_stmt FROM @zentide_owner_key_sql;
EXECUTE zentide_owner_key_stmt;
DEALLOCATE PREPARE zentide_owner_key_stmt;

SET @zentide_owner_fk_exists := (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='ow_zentide_hub' AND constraint_name='fk_zentide_hub_owner');
SET @zentide_owner_fk_sql := IF(@zentide_owner_fk_exists=0, 'ALTER TABLE ow_zentide_hub ADD CONSTRAINT fk_zentide_hub_owner FOREIGN KEY (owner_id) REFERENCES user_info(user_id) ON DELETE SET NULL', 'SELECT 1');
PREPARE zentide_owner_fk_stmt FROM @zentide_owner_fk_sql;
EXECUTE zentide_owner_fk_stmt;
DEALLOCATE PREPARE zentide_owner_fk_stmt;
