-- The ow_ namespace belonged to the former OfferWatch product name.
-- Rename every current Zentide table atomically while preserving all rows,
-- indexes and foreign-key relationships. Re-running against a V37 snapshot is safe.

SET @zentide_v37_previous_group_concat_max_len = @@SESSION.group_concat_max_len;
SET SESSION group_concat_max_len = 65535;

SELECT GROUP_CONCAT(
         CONCAT('`', table_name, '` TO `', SUBSTRING(table_name, 4), '`')
         ORDER BY table_name
         SEPARATOR ', '
       )
INTO @zentide_v37_rename_pairs
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name REGEXP '^ow_zentide_';

SET @zentide_v37_sql = IF(
  @zentide_v37_rename_pairs IS NULL,
  'DO 0',
  CONCAT('RENAME TABLE ', @zentide_v37_rename_pairs)
);

PREPARE zentide_v37_stmt FROM @zentide_v37_sql;
EXECUTE zentide_v37_stmt;
DEALLOCATE PREPARE zentide_v37_stmt;

SET SESSION group_concat_max_len = @zentide_v37_previous_group_concat_max_len;
SET @zentide_v37_previous_group_concat_max_len = NULL;
SET @zentide_v37_rename_pairs = NULL;
SET @zentide_v37_sql = NULL;
