ALTER TABLE ow_notification_batch
  ADD COLUMN retry_count INT UNSIGNED NOT NULL DEFAULT 0 AFTER item_count,
  ADD COLUMN failure_code VARCHAR(64) NULL AFTER digest_notification_id,
  ADD COLUMN failure_message VARCHAR(1000) NULL AFTER failure_code,
  ADD COLUMN next_retry_at DATETIME(3) NULL AFTER failure_message,
  ADD COLUMN dead_letter_at DATETIME(3) NULL AFTER next_retry_at;

UPDATE ow_platform_metadata
SET metadata_value='notification-delivery-reliability'
WHERE metadata_key='platform_phase';
