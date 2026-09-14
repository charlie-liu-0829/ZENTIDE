ALTER TABLE ow_zentide_radar
  ADD COLUMN attention_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER name,
  ADD COLUMN notification_strategy VARCHAR(24) NOT NULL DEFAULT 'DAILY' AFTER attention_level,
  ADD COLUMN change_types_json JSON NULL AFTER notification_strategy,
  ADD COLUMN source_preferences_json JSON NULL AFTER change_types_json,
  ADD COLUMN ignore_rules_json JSON NULL AFTER source_preferences_json,
  ADD COLUMN user_context VARCHAR(1000) NULL AFTER ignore_rules_json;

UPDATE ow_zentide_radar
SET change_types_json=JSON_ARRAY('RELEASE','BREAKING_CHANGE','SECURITY'),
    source_preferences_json=JSON_ARRAY('ALL'),
    ignore_rules_json=JSON_ARRAY()
WHERE change_types_json IS NULL;

UPDATE ow_platform_metadata
SET metadata_value='zentide-radar-observation-plan'
WHERE metadata_key='platform_phase';
