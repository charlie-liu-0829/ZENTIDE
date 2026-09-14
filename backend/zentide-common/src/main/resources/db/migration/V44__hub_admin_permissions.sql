-- Add per-interest-hub administrator permissions.
-- JSON keeps the compact schema while allowing the permission set to evolve.
SET @zentide_has_permissions_json := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'zentide_hub_member' AND column_name = 'permissions_json'
);
SET @zentide_add_permissions_sql := IF(
  @zentide_has_permissions_json = 0,
  'ALTER TABLE zentide_hub_member ADD COLUMN permissions_json JSON NULL AFTER role',
  'SELECT 1'
);
PREPARE zentide_permissions_stmt FROM @zentide_add_permissions_sql;
EXECUTE zentide_permissions_stmt;
DEALLOCATE PREPARE zentide_permissions_stmt;

UPDATE zentide_hub_member
SET permissions_json = '[]'
WHERE role = 'ADMIN' AND permissions_json IS NULL;
