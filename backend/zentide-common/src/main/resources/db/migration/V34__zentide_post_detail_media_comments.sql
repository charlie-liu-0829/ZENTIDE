ALTER TABLE ow_zentide_interest_post
  ADD COLUMN cover_url VARCHAR(2048) NULL AFTER media_json;

ALTER TABLE ow_zentide_interest_comment
  ADD COLUMN like_count INT UNSIGNED NOT NULL DEFAULT 0 AFTER status;

CREATE TABLE IF NOT EXISTS ow_zentide_interest_comment_reaction (
  comment_id BIGINT UNSIGNED NOT NULL,
  user_id VARCHAR(10) NOT NULL,
  reaction VARCHAR(20) NOT NULL DEFAULT 'LIKE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (comment_id, user_id),
  KEY idx_zentide_comment_reaction_user (user_id, created_at),
  CONSTRAINT fk_zentide_comment_reaction_comment FOREIGN KEY (comment_id) REFERENCES ow_zentide_interest_comment(comment_id) ON DELETE CASCADE,
  CONSTRAINT fk_zentide_comment_reaction_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
