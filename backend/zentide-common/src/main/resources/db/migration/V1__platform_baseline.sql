CREATE TABLE IF NOT EXISTS ow_platform_metadata (
    metadata_key VARCHAR(64) NOT NULL,
    metadata_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (metadata_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ow_platform_metadata (metadata_key, metadata_value)
VALUES ('platform_phase', 'foundation')
ON DUPLICATE KEY UPDATE metadata_value = VALUES(metadata_value);
