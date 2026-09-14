CREATE TABLE IF NOT EXISTS ow_zentide_retrieval_chunk (
  chunk_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  document_version_id BIGINT UNSIGNED NOT NULL,
  chunk_no INT UNSIGNED NOT NULL,
  content_start INT UNSIGNED NOT NULL,
  content_end INT UNSIGNED NOT NULL,
  chunk_text TEXT NOT NULL,
  content_hash CHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  indexed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(chunk_id),
  UNIQUE KEY uk_zentide_retrieval_chunk_version_no(document_version_id,chunk_no),
  KEY idx_zentide_retrieval_chunk_status(status,document_version_id),
  CONSTRAINT fk_zentide_retrieval_chunk_version FOREIGN KEY(document_version_id) REFERENCES ow_zentide_document_version(document_version_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE ow_platform_metadata
SET metadata_value='zentide-retrieval-v1'
WHERE metadata_key='platform_phase';
