-- Add the lightweight aggregate used by the user-configurable daily selection.
-- The project deliberately keeps this on the post table instead of introducing
-- another high-volume browsing-event table.
SET @has_view_count := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='zentide_interest_post' AND column_name='view_count'
);
SET @sql := IF(
  @has_view_count=0,
  'ALTER TABLE zentide_interest_post ADD COLUMN view_count INT UNSIGNED NOT NULL DEFAULT 0 AFTER bookmark_count',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
